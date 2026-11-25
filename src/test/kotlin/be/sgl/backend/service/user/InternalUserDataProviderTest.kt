package be.sgl.backend.service.user

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

/**
 * Also covers the shared behavior of [UserDataProvider].
 */
@ExtendWith(MockitoExtension::class)
class InternalUserDataProviderTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var accessApi: AccessApi
    @InjectMocks
    lateinit var testee: InternalUserDataProvider

    @Nested
    inner class AcceptRegistration {

        @Test
        fun `should generate a username and save the user when the user has no username yet`() {
            // given
            val user = user().name("Doe").firstName("John").build()

            // when
            testee.acceptRegistration(user)

            // then
            assertThat(user.username).isEqualTo("Doe.John")
            then(userRepository).should().save(user)
        }

        @Test
        fun `should fail when the user already has a username`() {
            // given
            val user = user().username("jdoe").build()

            // when / then
            assertThatThrownBy { testee.acceptRegistration(user) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Registration acceptance should not be performed on already known users!")
            then(userRepository).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class StartRole {

        @Test
        fun `should return true when the user does not have the role yet`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).build()
            given(accessApi.assign(user.id!!, role.id!!)).willReturn(true)

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when the user already has the role`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).build()
            given(accessApi.assign(user.id!!, role.id!!)).willReturn(false)

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isFalse()
        }
    }

    @Test
    fun `should revoke the role when ending a role`() {
        // given
        val user = user().build()
        val role = staffRole(branch().build()).build()

        // when
        testee.endRole(user, role)

        // then
        then(accessApi).should().revoke(user.id!!, role.id!!)
    }

    @Test
    fun `should save the user when updating a user`() {
        // given
        val user = user().build()
        given(userRepository.save(user)).willReturn(user)

        // when
        val result = testee.updateUser(user)

        // then
        assertThat(result).isSameAs(user)
    }
}
