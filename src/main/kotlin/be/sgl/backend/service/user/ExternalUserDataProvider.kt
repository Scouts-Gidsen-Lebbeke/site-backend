package be.sgl.backend.service.user

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.*
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.*
import be.sgl.backend.service.user.sync.CreateExternalFunctions
import be.sgl.backend.service.user.sync.EndExternalFunctions
import be.sgl.backend.util.ForExternalOrganization
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@ForExternalOrganization
class ExternalUserDataProvider(
    @Value("\${organization.external.id}")
    val externalOrganizationId: String,
    val lidaanvragenApi: LidaanvragenApi,
    val ledenApi: LedenApi,
    val createExternalFunctions: CreateExternalFunctions,
    val endExternalFunctions: EndExternalFunctions
) : UserDataProvider() {

    private val logger = KotlinLogging.logger {}

    override fun acceptRegistration(user: User) {
        logger.debug { "Accepting registration for user ${user.id}..." }
        val address = user.addresses.first()
        val lidAanvraag = LidAanvraag().apply {
            groepsnummer = externalOrganizationId
            opmerkingen = "Generated via sgl-backend"
            voornaam = user.firstName
            achternaam = user.name
            geboortedatum = user.birthdate
            persoonsgegevens = PersoonsGegevens().apply {
                geslacht = user.sex.toDto()
                gsm = user.mobile
            }
            email = user.email
            adres = address.toDto()
            verminderdlidgeld = user.hasReduction
        }
        lidaanvragenApi.postNieuweAanvragen(lidAanvraag)
        logger.debug { "External registration finished: request created and ready to be approved!" }
    }

    override fun updateUser(user: User): User {
        super.updateUser(user)
        ledenApi.patchLid(user.externalId, true, user.toDto())
        return user
    }

    override fun startRole(user: User, role: Role): UserRole? {
        val newRole = super.startRole(user, role) ?: return null
        // A lot easier than a check on authorization, externally synchronized roles are scaffolded
        // at membership creation, so 99% of the time in a non-admin user request
        if (role.memberRole) {
            logger.info { "Staring a role synchronized automatically, skipping external synchronization." }
            return newRole
        }
        createExternalFunctions.execute(user.externalId!!, role.externalId, role.backupExternalId)
        return newRole
    }

    override fun endRole(userRole: UserRole) {
        super.endRole(userRole)
        endExternalFunctions.execute(userRole.user.externalId!!, userRole.role.externalId, userRole.role.backupExternalId)
    }

    companion object {

        fun User.toDto() = Lid().apply {
            persoonsgegevens = PersoonsGegevens().apply {
                geslacht = sex.toDto()
                gsm = mobile
                rekeningnummer = accountNo
                rijksregisternummer = nis
            }
            vgagegevens = VgaGegevens().apply {
                voornaam = firstName
                achternaam = name
                beperking = hasHandicap
                verminderdlidgeld = hasReduction
                geboortedatum = birthdate
            }
            email = this@toDto.email
            adressen = addresses.map { it.toDto() }
            contacten = contacts.map { it.toDto() }
        }

        private fun Sex.toDto(): PersoonsGegevens.GeslachtEnum {
            return when(this) {
                Sex.MALE -> PersoonsGegevens.GeslachtEnum.MAN
                Sex.FEMALE -> PersoonsGegevens.GeslachtEnum.VROUW
                else -> PersoonsGegevens.GeslachtEnum.ANDERE
            }
        }

        private fun Address.toDto() = Adres().apply {
            id = externalId
            land = country
            postcode = zipcode
            gemeente = town
            straat = street
            nummer = number
            bus = subPremise
            postadres = postalAdress
            omschrijving = description
            status = Adres.StatusEnum.NORMAAL
        }

        private fun Contact.toDto() = be.sgl.backend.openapi.model.Contact().apply {
            id = externalId
            voornaam = firstName
            achternaam = name
            rol = when(role) {
                ContactRole.FATHER -> be.sgl.backend.openapi.model.Contact.RolEnum.VADER
                ContactRole.MOTHER -> be.sgl.backend.openapi.model.Contact.RolEnum.MOEDER
                ContactRole.GUARDIAN -> be.sgl.backend.openapi.model.Contact.RolEnum.VOOGD
                else -> be.sgl.backend.openapi.model.Contact.RolEnum.OPVOEDINGSVERANTWOORDELIJKE
            }
            gsm = mobile
            email = this@toDto.email
            rijksregisternummer = nis
            lidtenlaste = nis != null
            adres = address?.externalId
        }
    }
}