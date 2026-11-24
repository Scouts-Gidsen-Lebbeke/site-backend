package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.Contact
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
            val address = userToUpdate.addresses.firstOrNull { it.externalId == a.id }
                ?: Address().also(userToUpdate.addresses::add)
            address.externalId = a.id
            address.country = a.land
            address.zipcode = a.postcode
            address.town = a.gemeente
            address.street = a.straat
            address.number = a.nummer
            address.subPremise = a.bus?.takeIf { it.isNotBlank() }
            address.postalAdress = a.postadres
        }
        userRepository.save(userToUpdate)
        val unmatchedContacts = contactRepository.findContactsByUser(userToUpdate).toMutableList()
        lid.contacten.map { c ->
            // Contacts saved before their Groepsadmin id was known (older syncs, or created locally and pushed since)
            // are matched on name, so they get linked instead of duplicated.
            val contact = unmatchedContacts.firstOrNull { it.externalId != null && it.externalId == c.id }
                ?: unmatchedContacts.firstOrNull { it.externalId == null && it.firstName == c.voornaam && it.name == c.achternaam }
                ?: Contact()
            unmatchedContacts.remove(contact)
            contact.updateFrom(c, userToUpdate)
        }.forEach(contactRepository::save)
        // Linked contacts Groepsadmin no longer has were removed there. Unlinked ones are local and not pushed yet.
        contactRepository.deleteAll(unmatchedContacts.filter { it.externalId != null })
    }
}