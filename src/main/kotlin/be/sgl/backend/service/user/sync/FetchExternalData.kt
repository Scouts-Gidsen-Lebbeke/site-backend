package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.repository.user.UserRepository

@ExternalUsecase
class FetchExternalData(
    private val userRepository: UserRepository,
    private val ledenApi: LedenApi
) {

    fun execute(user: User) {
        val lid = ledenApi.getLid(user.externalId ?: return)
        user.mobile = lid.persoonsgegevens.gsm
        user.accountNo = lid.persoonsgegevens.rekeningnummer
        user.username?.let { user.memberId = lid.verbondsgegevens.lidnummer }
        lid.adressen.map { a ->
            val address = user.addresses.firstOrNull { it.externalId == a.id } ?: Address()
            address.externalId = a.id
            address.country = a.land
            address.zipcode = a.postcode
            address.town = a.gemeente
            address.street = a.straat
            address.number = a.nummer
            address.subPremise = a.bus?.takeIf { it.isNotBlank() }
            address.postalAdress = a.postadres
            user.addresses.add(address)
        }
        lid.contacten.filter { it.voornaam != null && it.achternaam != null }.map { c ->
            val contact = user.contacts.firstOrNull { it.externalId == c.id } ?: Contact()
            contact.externalId = c.id
            contact.firstName = c.voornaam
            contact.name = c.achternaam
            contact.role = when(c.rol) {
                be.sgl.backend.openapi.model.Contact.RolEnum.VADER -> ContactRole.FATHER
                be.sgl.backend.openapi.model.Contact.RolEnum.MOEDER -> ContactRole.MOTHER
                be.sgl.backend.openapi.model.Contact.RolEnum.VOOGD -> ContactRole.GUARDIAN
                else -> ContactRole.RESPONSIBLE
            }
            contact.mobile = c.gsm
            contact.email = c.email
            contact.address = user.addresses.firstOrNull { it.externalId == c.adres }
            user.contacts.add(contact)
        }
        userRepository.save(user)
    }
}