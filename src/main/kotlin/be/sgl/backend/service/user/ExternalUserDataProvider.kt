package be.sgl.backend.service.user

import be.sgl.backend.shared.Sex
import be.sgl.backend.shared.address.Address
import be.sgl.backend.people.domain.*
import be.sgl.backend.people.domain.Contact
import be.sgl.backend.shared.exception.LocalizedException
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.api.LidaanvragenApi
import be.sgl.backend.openapi.model.*
import be.sgl.backend.people.application.UserDataProvider
import be.sgl.backend.people.domain.ContactRepository
import be.sgl.backend.shared.util.ForExternalOrganization
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@ForExternalOrganization
class ExternalUserDataProvider(
    @Value("\${organization.external.id}")
    val externalOrganizationId: String,
    val lidaanvragenApi: LidaanvragenApi,
    val ledenApi: LedenApi,
    private val contactRepository: ContactRepository,
    userRepository: UserRepository
) : UserDataProvider(userRepository) {

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
        val externalId = user.externalId ?: run {
            logger.warn { "User ${user.username} is not linked to Groepsadmin, skipping external update." }
            return super.updateUser(user)
        }
        val lid = user.toDto(contactRepository.findContactsByUser(user))
        super.updateUser(user)
        ledenApi.patchLid(externalId, true, lid)
        return user
    }

    companion object {

        fun User.toDto(contacts: List<Contact>) = Lid().apply {
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
                verhoogdekinderbijslag = false
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
            val knownAddresses = user.addresses.filter { it.externalId != null }
            val contactAddress = address?.takeIf { it.externalId != null }
                ?: knownAddresses.firstOrNull { it.postalAdress } ?: knownAddresses.firstOrNull()
            adres = contactAddress?.externalId ?: throw LocalizedException("user.contact.address.unknown", firstName, name)
        }
    }
}