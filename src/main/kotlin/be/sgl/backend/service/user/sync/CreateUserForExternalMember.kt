package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.*
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.openapi.model.PersoonsGegevens
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.repository.user.UserRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

@ExternalUsecase
class CreateUserForExternalMember{

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var ledenApi: LedenApi
    @Autowired
    private lateinit var userRepository: UserRepository
    @Value("\${organization.external.id}")
    lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    fun execute(externalId: String) {
        logger.info { "Creating new user based on external id $externalId..." }
        val externalMember = ledenApi.getLid(externalId)
        var user = User().apply {
            roles.addAll(externalMember.functies.mapNotNull { f -> translateFunction(this, f) })
            sex = when(externalMember.persoonsgegevens.geslacht) {
                PersoonsGegevens.GeslachtEnum.MAN -> Sex.MALE
                PersoonsGegevens.GeslachtEnum.VROUW -> Sex.FEMALE
                else -> Sex.UNKNOWN
            }
            mobile = externalMember.persoonsgegevens.gsm
            hasHandicap = externalMember.vgagegevens.beperking
            hasReduction = externalMember.vgagegevens.verminderdlidgeld
            accountNo = externalMember.persoonsgegevens.rekeningnummer
            birthdate = externalMember.vgagegevens.geboortedatum
            memberId = externalMember.verbondsgegevens.lidnummer
            nis = externalMember.persoonsgegevens.rijksregisternummer
            addresses.addAll(externalMember.adressen.map { a -> Address().apply {
                this.externalId = a.id
                street = a.straat
                number = a.nummer
                subPremise = a.bus
                zipcode = a.postcode
                town = a.gemeente
                country = a.land
                description = a.omschrijving
                postalAdress = a.postadres
            } } )
            contacts.addAll(externalMember.contacten.map { c -> Contact().apply {
                name = c.achternaam
                firstName = c.voornaam
                role = when(c.rol) {
                    be.sgl.backend.openapi.model.Contact.RolEnum.VADER -> ContactRole.FATHER
                    be.sgl.backend.openapi.model.Contact.RolEnum.MOEDER -> ContactRole.MOTHER
                    be.sgl.backend.openapi.model.Contact.RolEnum.VOOGD -> ContactRole.GUARDIAN
                    else -> ContactRole.RESPONSIBLE
                }
                address = addresses.firstOrNull { a -> a.externalId == c.adres }
                mobile = c.gsm
                email = c.email
                // will never come through
                // nis = c.rijksregisternummer
            } } )
        }
        user = userRepository.save(user)
        logger.info { "User created, translating medical record..." }
        val externalMedicalRecord = ledenApi.getLidSteekkaart(externalId, null).gegevens
        val medicalRecord = MedicalRecord().apply {
            this.user = user
            mayBePhotographed = externalMedicalRecord.waarden["d5f75e1e463384de014639190ebb00eb"] == "ja"
            mayTakePainkillers = externalMedicalRecord.waarden["d5f75e1e463384de0146390e395900e2"] == "ja"
            // Zo ja, op vlak van voeding (vb. vegetariër, halal):
            foodAnomalies = externalMedicalRecord.waarden["d5f75e1e463384de0146391a3b4800ed"].sanitized()
            // Onze zoon of dochter moet een bepaald dieet volgen
            allergies = externalMedicalRecord.waarden["d5f75e1e463384de0146391124af00e5"].sanitized()
            val impossibleActivities = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f119f740016"].sanitized()
            val atSports = externalMedicalRecord.waarden["d5f75e1e480b9aa901480c7fb70100de"].sanitized()
            val atHygiene = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f1464ef001a"].sanitized()
            val atSocial = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f14d9de001b"].sanitized()
            val other = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f1523c6001c"].sanitized()
            activityRestrictions = listOfNotNull(impossibleActivities, atSports, atHygiene, atSocial, other).joinToString(",").sanitized()
            familyRemarks = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f026f8e0013"].sanitized()
            // Zo ja, andere aandachtspunten die belang kunnen hebben bij de omgang met ons kind:
            socialRemarks = externalMedicalRecord.waarden["d5f75e1e463384de0146391abdd000ee"].sanitized()
            val diseaseList = externalMedicalRecord.waarden["d5f75e1e463384de01463905280100de"].sanitized()
            val diseaseGuidance = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f1464ef001a"].sanitized()
            diseases = listOfNotNull(diseaseList, diseaseGuidance).joinToString(", ").sanitized()
            // ja/nee, but no concrete info
            medications = externalMedicalRecord.waarden["d5f75e1e463384de01463901e13c00dc"].sanitized()
            // physician field not derivable
            physicianContact = externalMedicalRecord.waarden["d5f75e1e463384de0146391800f100e9"].sanitized()
            bloodGroup = when(externalMedicalRecord.waarden["d5f75e1e463384de01463916d21b00e8"]) {
                "O+" -> BloodGroup.OP
                "O-" -> BloodGroup.ON
                "A+" -> BloodGroup.AP
                "A-" -> BloodGroup.AN
                "B+ " -> BloodGroup.BP
                "B-" -> BloodGroup.BN
                "AB+" -> BloodGroup.ABP
                "AB-" -> BloodGroup.ABN
                else -> BloodGroup.UNKNOWN
            }
        }
        medicalRecordRepository.save(medicalRecord)
        logger.info { "Medical record created" }
    }

    private fun translateFunction(user: User, function: FunctieInstantie): UserRole? {
        if (function.groep != externalOrganizationId) return null
        val role = roleRepository.getRoleByExternalIdEquals(function.functie) ?: return null
        return UserRole(user, role)
    }

    private fun String?.sanitized(): String? {
        return this?.takeIf { it.isNotBlank() && it != "/" && !it.equals("nee", true)
                && !it.equals("neen", true) && !it.equals("nvt", true)
                && !it.equals("geen", true) }
    }
}