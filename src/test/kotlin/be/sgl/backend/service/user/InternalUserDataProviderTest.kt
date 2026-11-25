package be.sgl.backend.service.user

import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.role.UserRoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.util.anyValue
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
    lateinit var userRoleRepository: UserRoleRepository
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
        fun `should save and add the role when the user does not have it yet`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).build()
            given(userRoleRepository.save(anyValue<UserRole>())).willAnswer { it.arguments[0] }

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result!!.role).isSameAs(role)
            assertThat(user.roles).containsExactly(result)
        }

        @Test
        fun `should return null and not save when the user already has the role`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).build()
            userRole().user(user).role(role).build()

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isNull()
            assertThat(user.roles).hasSize(1)
            then(userRoleRepository).shouldHaveNoInteractions()
        }
    }

    @Test
    fun `should delete the user role when ending a role`() {
        // given
        val userRole = userRole().build()

        // when
        testee.endRole(userRole)

        // then
        then(userRoleRepository).should().delete(userRole)
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
