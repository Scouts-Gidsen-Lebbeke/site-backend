package be.sgl.backend.service.user

import be.sgl.backend.dto.user.ContactDTO
import be.sgl.backend.dto.user.CreateOrUpdateContactRequest
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.entity.user.User
import be.sgl.backend.shared.exception.AddressNotFoundException
import be.sgl.backend.shared.exception.ContactNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.repository.AddressRepository
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.repository.user.UserRepository
import org.springframework.stereotype.Service

@Service
class ContactService(
    private val contactRepository: ContactRepository,
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository
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
        val currentUser = userRepository.getByUsername(currentUsername)
        if (currentUser.level != RoleLevel.ADMIN && currentUser.username != userFromContact.username) {
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