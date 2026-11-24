package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.role.UserRole
import be.sgl.backend.entity.user.*
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.FunctieInstantie
import be.sgl.backend.openapi.model.PersoonsGegevens
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.repository.user.UserRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import java.time.OffsetDateTime

@ExternalUsecase
class CreateUserForExternalMember(
    private val ledenApi: LedenApi,
    private val userRepository: UserRepository,
    @Value("\${organization.external.id}")
    private var externalOrganizationId: String,
    private val roleRepository: RoleRepository,
    private val contactRepository: ContactRepository
) {

    private val logger = KotlinLogging.logger {}

    fun execute(externalId: String): User {
        logger.info { "Creating new user based on external id $externalId..." }
        val externalMember = ledenApi.getLid(externalId)
        var newUser = User().apply {
            this.externalId = externalMember.id
            name = externalMember.vgagegevens.achternaam
            firstName = externalMember.vgagegevens.voornaam
            email = externalMember.email
            roles.addAll(externalMember.functies.mapNotNull { f -> translateFunction(this, f) })
            sex = when(externalMember.persoonsgegevens.geslacht) {
                PersoonsGegevens.GeslachtEnum.MAN -> Sex.MALE
                PersoonsGegevens.GeslachtEnum.VROUW -> Sex.FEMALE
                else -> Sex.UNKNOWN
            }
            username = externalMember.gebruikersnaam
            mobile = externalMember.persoonsgegevens.gsm
            hasHandicap = externalMember.vgagegevens.beperking
            hasReduction = externalMember.vgagegevens.verminderdlidgeld
            accountNo = externalMember.persoonsgegevens.rekeningnummer
            birthdate = externalMember.vgagegevens.geboortedatum
            memberId = externalMember.verbondsgegevens.lidnummer
            addresses.addAll(externalMember.adressen.map { a -> Address().apply {
                this.externalId = a.id
                street = a.straat
                number = a.nummer
                subPremise = a.bus
                zipcode = a.postcode
                town = a.gemeente
                country = a.land
                postalAdress = a.postadres
            } } )
        }
        newUser = userRepository.save(newUser)
        externalMember.contacten.map { Contact().updateFrom(it, newUser) }.forEach(contactRepository::save)
        return newUser
    }

    private fun translateFunction(user: User, function: FunctieInstantie): UserRole? {
        if (function.groep != externalOrganizationId) return null
        if (function.einde?.isBefore(OffsetDateTime.now()) == true) return null
        val role = roleRepository.getRoleByExternalIdEquals(function.functie) ?: return null
        return UserRole(user, role)
    }
}