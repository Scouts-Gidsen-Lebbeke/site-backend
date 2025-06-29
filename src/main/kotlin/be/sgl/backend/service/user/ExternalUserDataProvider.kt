package be.sgl.backend.service.user

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.*
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.*
import be.sgl.backend.util.ForExternalOrganization
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.*

@Service
@ForExternalOrganization
class ExternalUserDataProvider : UserDataProvider() {

    private val logger = KotlinLogging.logger {}

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

    override fun findUser(username: String): User? {
        // TODO: remove and handle lazy fetching of contacts/addresses
        return userRepository.findByUsername(username)
    }

    override fun findByNameAndEmail(name: String, firstName: String, email: String): User? {
        // TODO: remove and handle lazy fetching of contacts/addresses
        return userRepository.findByNameAndFirstNameAndEmail(name, firstName, email)
    }

    override fun updateUser(user: User): User {
        super.updateUser(user)
        ledenApi.patchLid(user.externalId, true, user.toDto())
        return user
    }

    override fun startRole(user: User, role: Role): UserRole? {
        val newRole = super.startRole(user, role) ?: return null
        // Much cleaner than a check on authorization, externally synchronized roles are scaffolded
        // at membership creation, so 99% of the time in a non-admin user request
        if (role.forExternalSync) {
            logger.info { "Staring a role synchronized automatically, skipping external synchronization." }
            return newRole
        }
        if (role.externalId == null) {
            logger.info { "Starting a non-externally linked role ${role.name}, skipped external synchronization." }
            return newRole
        }
        val lidPatch = Lid()
        lidPatch.functies.add(FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = role.externalId
            begin = OffsetDateTime.of(newRole.startDate, LocalTime.MIN, ZoneOffset.UTC)
        })
        role.backupExternalId?.let {
            logger.debug { "${user.username} has a back-up external id, also adding this role." }
            lidPatch.functies.add(FunctieInstantie().apply {
                groep = externalOrganizationId
                functie = it
                begin = OffsetDateTime.of(newRole.startDate, LocalTime.MIN, ZoneOffset.UTC)
            })
        }
        ledenApi.patchLid(user.externalId!!, true, lidPatch)
        return newRole
    }

    override fun endRole(user: User, role: Role): UserRole? {
        val userRole = super.startRole(user, role) ?: return null
        if (role.externalId == null) {
            logger.info { "Ending a non-externally linked role ${role.name}, skipped external synchronization." }
            return userRole
        }
        val lidPatch = Lid()
        lidPatch.functies.add(FunctieInstantie().apply {
            groep = externalOrganizationId
            functie = role.externalId
            begin = OffsetDateTime.of(userRole.startDate, LocalTime.MIN, ZoneOffset.UTC)
            einde = OffsetDateTime.of(userRole.endDate, LocalTime.MIN, ZoneOffset.UTC)
        })
        role.backupExternalId?.let {
            logger.debug { "${user.username} has a back-up external id, also removing this role." }
            lidPatch.functies.add(FunctieInstantie().apply {
                groep = externalOrganizationId
                functie = it
                begin = OffsetDateTime.of(userRole.startDate, LocalTime.MIN, ZoneOffset.UTC)
                einde = OffsetDateTime.of(userRole.endDate, LocalTime.MIN, ZoneOffset.UTC)
            })
        }
        ledenApi.patchLid(user.externalId!!, true, lidPatch)
        return userRole
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
                beperking = hasHandicap
                verminderdlidgeld = hasReduction
                geboortedatum = birthdate
            }
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
            lidtenlaste = taxable
            adres = address?.externalId
        }
    }
}