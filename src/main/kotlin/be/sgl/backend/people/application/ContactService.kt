package be.sgl.backend.people.application

import be.sgl.backend.people.web.ContactDTO
import be.sgl.backend.people.web.CreateOrUpdateContactRequest
import be.sgl.backend.shared.address.Address
import be.sgl.backend.people.domain.Contact
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.security.UserLevelProvider
import be.sgl.backend.people.domain.User
import be.sgl.backend.shared.exception.AddressNotFoundException
import be.sgl.backend.shared.exception.ContactNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.shared.address.AddressRepository
import be.sgl.backend.people.domain.ContactRepository
import be.sgl.backend.people.domain.UserRepository
import org.springframework.stereotype.Service

@Service
class ContactService(
    private val contactRepository: ContactRepository,
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository,
    private val userLevelProvider: UserLevelProvider
) {

    fun getContactsForUser(userId: Int, currentUsername: String): List<ContactDTO> {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException("") }
        validateOwnerOrAdmin(user, currentUsername)
        return contactRepository.findContactsByUser(user).map(ContactDTO::of)
    }

    fun getContactDTOById(id: Int, currentUsername: String): ContactDTO {
        val contact = getContactById(id)
        validateOwnerOrAdmin(contact.user, currentUsername)
        return ContactDTO.of(contact)
    }

    fun createContact(request: CreateOrUpdateContactRequest, currentUsername: String): ContactDTO {
        val newContact = request.toEntity()
        newContact.user = userRepository.findById(request.userId!!)
            .orElseThrow { UserNotFoundException("") }
        validateOwnerOrAdmin(newContact.user, currentUsername)
        request.addressId?.let { newContact.address = getAddressById(it) }
        return ContactDTO.of(contactRepository.save(newContact))
    }

    fun updateContact(id: Int, request: CreateOrUpdateContactRequest, currentUsername: String): ContactDTO {
        val contactToUpdate = getContactById(id)
        validateOwnerOrAdmin(contactToUpdate.user, currentUsername)
        val contactFromRequest = request.toEntity()
        contactToUpdate.firstName = contactFromRequest.firstName
        contactToUpdate.name = contactFromRequest.name
        contactToUpdate.role = contactFromRequest.role
        contactToUpdate.mobile = contactFromRequest.mobile
        contactToUpdate.email = contactFromRequest.email
        contactToUpdate.nis = contactFromRequest.nis
        request.addressId?.let { contactToUpdate.address = getAddressById(it) }
        return ContactDTO.of(contactRepository.save(contactToUpdate))
    }

    fun deleteContact(id: Int, currentUsername: String) {
        val contactToDelete = getContactById(id)
        validateOwnerOrAdmin(contactToDelete.user, currentUsername)
        contactRepository.delete(contactToDelete)
    }

    private fun validateOwnerOrAdmin(userFromContact: User, currentUsername: String) {
        if (currentUsername != userFromContact.username && userLevelProvider.levelOf(currentUsername) != RoleLevel.ADMIN) {
            throw IllegalAccessError("Current user cannot access this contact!")
        }
    }

    private fun getContactById(id: Int): Contact {
        return contactRepository.findById(id).orElseThrow { ContactNotFoundException() }
    }

    private fun getAddressById(id: Int): Address {
        return addressRepository.findById(id).orElseThrow { throw AddressNotFoundException() }
    }
}