package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.repository.user.UserRepository

@ExternalUsecase
class FetchExternalData(
    private val userRepository: UserRepository,
    private val ledenApi: LedenApi,
    private val contactRepository: ContactRepository
) {

    fun execute(userToUpdate: User) {
        val lid = ledenApi.getLid(userToUpdate.externalId ?: return)
        userToUpdate.mobile = lid.persoonsgegevens.gsm
        userToUpdate.accountNo = lid.persoonsgegevens.rekeningnummer
        userToUpdate.username?.let { userToUpdate.memberId = lid.verbondsgegevens.lidnummer }
        lid.adressen.map { a ->
            val address = userToUpdate.addresses.firstOrNull { it.externalId == a.id } ?: Address()
            address.externalId = a.id
            address.country = a.land
            address.zipcode = a.postcode
            address.town = a.gemeente
            address.street = a.straat
            address.number = a.nummer
            address.subPremise = a.bus?.takeIf { it.isNotBlank() }
            address.postalAdress = a.postadres
            userToUpdate.addresses.add(address)
        }
        userRepository.save(userToUpdate)
        lid.contacten.map { c -> Contact().apply {
            name = c.achternaam
            firstName = c.voornaam
            user = userToUpdate
            role = when(c.rol) {
                be.sgl.backend.openapi.model.Contact.RolEnum.VADER -> ContactRole.FATHER
                be.sgl.backend.openapi.model.Contact.RolEnum.MOEDER -> ContactRole.MOTHER
                be.sgl.backend.openapi.model.Contact.RolEnum.VOOGD -> ContactRole.GUARDIAN
                else -> ContactRole.RESPONSIBLE
            }
            address = userToUpdate.addresses.firstOrNull { a -> a.externalId == c.adres }
            mobile = c.gsm
            email = c.email
        } }.forEach(contactRepository::save)
    }
}