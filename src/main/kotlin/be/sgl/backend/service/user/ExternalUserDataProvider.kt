package be.sgl.backend.service.user

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.*
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.*
import be.sgl.backend.service.exception.UserNotFoundException
import be.sgl.backend.util.ForExternalOrganization
import jakarta.persistence.EntityManager
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.*

@Service
@ForExternalOrganization
class ExternalUserDataProvider : UserDataProvider() {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var entityManager: EntityManager
    @Value("\${organization.external.id}")
    private lateinit var externalOrganizationId: String
    @Autowired
    private lateinit var ledenApi: LedenApi
    @Autowired
    private lateinit var lidaanvragenApi: LidaanvragenApi

    override fun acceptRegistration(user: User) {
        logger.debug { "Accepting registration for user ${user.id}..." }
        val address = user.addresses.first()
        val lidAanvraag = LidAanvraag().apply {
            groepsnummer = externalOrganizationId
            opmerkingen = "Generated via sgl-backend"
            voornaam = user.firstName
            achternaam = user.name
            geboortedatum = user.birthdate
            persoonsgegevens.geslacht = when (user.sex) {
                Sex.MALE -> PersoonsGegevens.GeslachtEnum.MAN
                Sex.FEMALE -> PersoonsGegevens.GeslachtEnum.VROUW
                Sex.UNKNOWN -> PersoonsGegevens.GeslachtEnum.ANDERE
            }
            persoonsgegevens.gsm = user.mobile
            email = user.email
            adres = Adres().apply {
                land = address.country
                postcode = address.zipcode
                gemeente = address.town
                straat = address.street
                nummer = address.number
                bus = address.subPremise
                postadres = true
                status = Adres.StatusEnum.NORMAAL
            }
            verminderdlidgeld = user.hasReduction
        }
        lidaanvragenApi.postNieuweAanvragen(lidAanvraag)
        logger.debug { "External registration finished: request created and ready to be approved!" }
    }

    override fun findUser(username: String): User? {
        return userRepository.findByUsername(username)?.withExternalData()
    }

    override fun getUser(username: String): User {
        return userRepository.findByUsername(username)?.withExternalData() ?: throw UserNotFoundException(username)
    }

    override fun findByNameAndEmail(name: String, firstName: String, email: String): User? {
        return userRepository.findByNameAndFirstNameAndEmail(name, firstName, email)?.withExternalData()
    }

    override fun updateUser(user: User): User {
        val persistedUser = userRepository.getReferenceById(user.id!!)
        persistedUser.firstName = user.firstName
        persistedUser.name = user.name
        persistedUser.email = user.email
        persistedUser.birthdate = user.birthdate
        persistedUser.image = user.image
        persistedUser.ageDeviation = user.ageDeviation
        persistedUser.sex = user.sex
        userRepository.save(persistedUser)
        // TODO: post rest to GA
        return user
    }

    override fun startRole(user: User, role: Role) {
        if (user.roles.none { it.role == role }) {
            logger.warn { "${user.username} already has the role ${role.name}! Starting aborted." }
            return
        }
        val newRole = UserRole(user, role)
        val externalRole = role.externalId
        if (externalRole == null) {
            logger.error { "Trying to end a non-externally linked role ${role.name}!" }
            return
        }
        val externalFunction = FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = externalRole
            begin = OffsetDateTime.of(newRole.startDate ?: return, LocalTime.MIN, ZoneOffset.UTC)
        }
        val lidPatch = Lid().apply {
            functies = mutableListOf(externalFunction)
        }
        role.backupExternalId?.let {
            logger.debug { "${user.username} has a back-up external id, also adding this role." }
            val backupExternalFunction = FunctieInstantie().apply {
                groep = externalOrganizationId
                functie = it
                begin = OffsetDateTime.of(newRole.startDate ?: return, LocalTime.MIN, ZoneOffset.UTC)
            }
            lidPatch.functies.add(backupExternalFunction)
        }
        ledenApi.patchLid(user.externalId!!, true, lidPatch)
        user.roles.add(newRole)
    }

    override fun endRole(user: User, role: Role) {
        val userRole = user.roles.find { it.role == role }
        if (userRole == null) {
            logger.warn { "${user.username} never had the role ${role.name}! Ending aborted." }
            return
        }
        val externalRole = role.externalId
        if (externalRole == null) {
            logger.error { "Trying to end a non-externally linked role ${role.name}!" }
            return
        }
        userRole.endDate = LocalDate.now()
        val externalFunction = FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = externalRole
            begin = OffsetDateTime.of(userRole.startDate ?: return, LocalTime.MIN, ZoneOffset.UTC)
            einde = OffsetDateTime.of(userRole.endDate ?: return, LocalTime.MIN, ZoneOffset.UTC)
        }
        val lidPatch = Lid().apply {
            functies = mutableListOf(externalFunction)
        }
        role.backupExternalId?.let {
            logger.debug { "${user.username} has a back-up external id, also removing this role." }
            val backupExternalFunction = FunctieInstantie().apply {
                groep = externalOrganizationId
                functie = it
                begin = OffsetDateTime.of(userRole.startDate ?: return, LocalTime.MIN, ZoneOffset.UTC)
                einde = OffsetDateTime.of(userRole.endDate ?: return, LocalTime.MIN, ZoneOffset.UTC)
            }
            lidPatch.functies.add(backupExternalFunction)
        }
        ledenApi.patchLid(user.externalId!!, true, lidPatch)
        user.roles.remove(userRole)
    }

    private fun User.withExternalData() = apply {
        ledenApi.getLid(externalId)?.let {
            roles.addAll(it.functies.mapNotNull { f -> translateFunction(this, f) })
            sex = when(it.persoonsgegevens.geslacht) {
                PersoonsGegevens.GeslachtEnum.MAN -> Sex.MALE
                PersoonsGegevens.GeslachtEnum.VROUW -> Sex.FEMALE
                else -> Sex.UNKNOWN
            }
            mobile = it.persoonsgegevens.gsm
            hasHandicap = it.vgagegevens.beperking
            hasReduction = it.vgagegevens.verminderdlidgeld
            accountNo = it.persoonsgegevens.rekeningnummer
            birthdate = it.vgagegevens.geboortedatum
            memberId = it.verbondsgegevens.lidnummer
            addresses.addAll(it.adressen.map { a -> Address().apply {
                externalId = a.id
                street = a.straat
                number = a.nummer
                subPremise = a.bus
                zipcode = a.postcode
                town = a.gemeente
                country = a.land
                description = a.omschrijving
                postalAdress = a.postadres
            } } )
            contacts.addAll(it.contacten.map { c ->
                val contact = Contact()
                contact.name = c.achternaam
                contact.firstName = c.voornaam
                contact.role = when(c.rol) {
                    be.sgl.backend.openapi.model.Contact.RolEnum.VADER -> ContactRole.FATHER
                    be.sgl.backend.openapi.model.Contact.RolEnum.MOEDER -> ContactRole.MOTHER
                    be.sgl.backend.openapi.model.Contact.RolEnum.VOOGD -> ContactRole.GUARDIAN
                    else -> ContactRole.RESPONSIBLE
                }
                contact.address = addresses.firstOrNull { a -> a.externalId == c.id }
                contact.mobile = c.gsm
                contact.email = c.email
                contact
            })
        }
        entityManager.detach(this)
    }

    private fun translateFunction(user: User, function: FunctieInstantie): UserRole? {
        if (function.groep != externalOrganizationId) return null
        val role = roleRepository.getRoleByExternalIdEquals(function.functie) ?: return null
        val endDate = function.einde?.toLocalDate()
        if (endDate != null && endDate < LocalDate.now()) return null
        return UserRole(user, role, function.begin.toLocalDate(), endDate)
    }
}