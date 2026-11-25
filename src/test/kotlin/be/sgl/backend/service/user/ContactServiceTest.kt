package be.sgl.backend.service.user

import be.sgl.backend.dto.user.CreateOrUpdateContactRequestTestMother.contactRequest
import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.ContactRole
import be.sgl.backend.entity.user.ContactTestMother.contact
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.AddressNotFoundException
import be.sgl.backend.shared.exception.ContactNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.shared.address.AddressRepository
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ContactServiceTest {

    @Mock
    lateinit var contactRepository: ContactRepository
    @Mock
    lateinit var addressRepository: AddressRepository
    @Mock
    lateinit var userRepository: UserRepository
    @InjectMocks
    lateinit var testee: ContactService

    private val owner = user().id(1).username(OWNER).build()
    private val stranger = user().id(2).username(STRANGER).build()

    private fun admin(): User {
        val admin = user().id(3).username(ADMIN).build()
        userRole().user(admin).role(adminRole().build()).build()
        return admin
    }

    @Nested
    inner class GetContactsForUser {

        @Test
        fun `should return the contacts when the owner requests them`() {
            // given
            val contact = contact().user(owner).build()
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)
            given(contactRepository.findContactsByUser(owner)).willReturn(listOf(contact))

            // when
            val result = testee.getContactsForUser(1, OWNER)

            // then
            assertThat(result).extracting("firstName").containsExactly(contact.firstName)
        }

        @Test
        fun `should return the contacts when an admin requests them`() {
            // given
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(ADMIN)).willReturn(admin())
            given(contactRepository.findContactsByUser(owner)).willReturn(listOf(contact().user(owner).build()))

            // when
            val result = testee.getContactsForUser(1, ADMIN)

            // then
            assertThat(result).hasSize(1)
        }

        @Test
        fun `should deny access when another non admin user requests them`() {
            // given
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(STRANGER)).willReturn(stranger)

            // when / then
            assertThatThrownBy { testee.getContactsForUser(1, STRANGER) }
                .isInstanceOf(IllegalAccessError::class.java)
                .hasMessage("Current user cannot access this contact!")
            then(contactRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw when the user does not exist`() {
            // given
            given(userRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getContactsForUser(1, OWNER) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    inner class GetContactDTOById {

        @Test
        fun `should return the contact when the owner requests it`() {
            // given
            val contact = contact().id(5).user(owner).build()
            given(contactRepository.findById(5)).willReturn(Optional.of(contact))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)

            // when
            val result = testee.getContactDTOById(5, OWNER)

            // then
            assertThat(result.id).isEqualTo(5)
        }

        @Test
        fun `should throw when the contact does not exist`() {
            // given
            given(contactRepository.findById(5)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getContactDTOById(5, OWNER) }
                .isInstanceOf(ContactNotFoundException::class.java)
                .hasMessage("Contact not found")
        }
    }

    @Nested
    inner class CreateContact {

        @Test
        fun `should create a contact linked to the user and address when the owner creates it`() {
            // given
            val address = address().build()
            val request = contactRequest().userId(1).addressId(9).role(ContactRole.GUARDIAN).build()
            val captor = argumentCaptor<Contact>()
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)
            given(addressRepository.findById(9)).willReturn(Optional.of(address))
            given(contactRepository.save(captureValue(captor))).willAnswer { it.arguments[0] }

            // when
            testee.createContact(request, OWNER)

            // then
            val saved = captor.value
            assertThat(saved.user).isSameAs(owner)
            assertThat(saved.address).isSameAs(address)
            assertThat(saved).extracting("firstName", "name", "role", "mobile", "email", "nis")
                .containsExactly(request.firstName, request.name, request.role, request.mobile, request.email, request.nis)
        }

        @Test
        fun `should create a contact without address when no address id is given`() {
            // given
            val request = contactRequest().userId(1).addressId(null).build()
            val captor = argumentCaptor<Contact>()
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)
            given(contactRepository.save(captureValue(captor))).willAnswer { it.arguments[0] }

            // when
            testee.createContact(request, OWNER)

            // then
            assertThat(captor.value.address).isNull()
            then(addressRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw when the address does not exist`() {
            // given
            val request = contactRequest().userId(1).addressId(9).build()
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)
            given(addressRepository.findById(9)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.createContact(request, OWNER) }
                .isInstanceOf(AddressNotFoundException::class.java)
            then(contactRepository).should(never()).save(anyValue())
        }

        @Test
        fun `should deny creation when a non admin creates a contact for another user`() {
            // given
            val request = contactRequest().userId(1).build()
            given(userRepository.findById(1)).willReturn(Optional.of(owner))
            given(userRepository.getByUsername(STRANGER)).willReturn(stranger)

            // when / then
            assertThatThrownBy { testee.createContact(request, STRANGER) }
                .isInstanceOf(IllegalAccessError::class.java)
            then(contactRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw when the user of the request does not exist`() {
            // given
            val request = contactRequest().userId(1).build()
            given(userRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.createContact(request, OWNER) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    inner class UpdateContact {

        @Test
        fun `should overwrite the contact fields and address when the owner updates it`() {
            // given
            val existing = contact().id(5).user(owner).role(ContactRole.MOTHER).mobile("old").build()
            val newAddress = address().street("New street").build()
            val request = contactRequest().role(ContactRole.FATHER).mobile(null).email("new@corp.com").nis("nis").addressId(9).build()
            given(contactRepository.findById(5)).willReturn(Optional.of(existing))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)
            given(addressRepository.findById(9)).willReturn(Optional.of(newAddress))
            given(contactRepository.save(existing)).willReturn(existing)

            // when
            testee.updateContact(5, request, OWNER)

            // then
            assertThat(existing).extracting("firstName", "name", "role", "mobile", "email", "nis", "address", "user")
                .containsExactly(request.firstName, request.name, ContactRole.FATHER, null, "new@corp.com", "nis", newAddress, owner)
        }

        @Test
        fun `should keep the existing address when no address id is given`() {
            // given
            val oldAddress = address().build()
            val existing = contact().id(5).user(owner).address(oldAddress).build()
            val request = contactRequest().addressId(null).build()
            given(contactRepository.findById(5)).willReturn(Optional.of(existing))
            given(userRepository.getByUsername(OWNER)).willReturn(owner)
            given(contactRepository.save(existing)).willReturn(existing)

            // when
            testee.updateContact(5, request, OWNER)

            // then
            assertThat(existing.address).isSameAs(oldAddress)
        }

        @Test
        fun `should deny the update when a non admin updates the contact of another user`() {
            // given
            val existing = contact().id(5).user(owner).build()
            given(contactRepository.findById(5)).willReturn(Optional.of(existing))
            given(userRepository.getByUsername(STRANGER)).willReturn(stranger)

            // when / then
            assertThatThrownBy { testee.updateContact(5, contactRequest().build(), STRANGER) }
                .isInstanceOf(IllegalAccessError::class.java)
            then(contactRepository).should(never()).save(anyValue())
        }
    }

    @Nested
    inner class DeleteContact {

        @Test
        fun `should delete the contact when an admin deletes it`() {
            // given
            val existing = contact().id(5).user(owner).build()
            given(contactRepository.findById(5)).willReturn(Optional.of(existing))
            given(userRepository.getByUsername(ADMIN)).willReturn(admin())

            // when
            testee.deleteContact(5, ADMIN)

            // then
            then(contactRepository).should().delete(existing)
        }

        @Test
        fun `should deny the deletion when a non admin deletes the contact of another user`() {
            // given
            val existing = contact().id(5).user(owner).build()
            given(contactRepository.findById(5)).willReturn(Optional.of(existing))
            given(userRepository.getByUsername(STRANGER)).willReturn(stranger)

            // when / then
            assertThatThrownBy { testee.deleteContact(5, STRANGER) }
                .isInstanceOf(IllegalAccessError::class.java)
            then(contactRepository).should(never()).delete(anyValue())
        }
    }

    companion object {
        private const val OWNER = "owner"
        private const val STRANGER = "stranger"
        private const val ADMIN = "admin"
    }
}
