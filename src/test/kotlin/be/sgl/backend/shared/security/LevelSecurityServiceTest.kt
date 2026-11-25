package be.sgl.backend.shared.security

import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
class LevelSecurityServiceTest {

    @Mock
    lateinit var userRepository: UserRepository
    @InjectMocks
    lateinit var testee: LevelSecurityService

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    private fun authenticateAs(username: String) {
        SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(username, null)
    }

    @Nested
    inner class IsAdmin {

        @Test
        fun `should be true when the current user has an admin role`() {
            // given
            authenticateAs(USERNAME)
            val user = user().username(USERNAME).build()
            userRole().user(user).role(adminRole().build()).build()
            given(userRepository.findByUsername(USERNAME)).willReturn(user)

            // when
            val isAdmin = testee.isAdmin()

            // then
            assertThat(isAdmin).isTrue()
        }

        @Test
        fun `should be false when the current user only has a staff role`() {
            // given
            authenticateAs(USERNAME)
            val user = user().username(USERNAME).build()
            userRole().user(user).role(staffRole(branch().build()).build()).build()
            given(userRepository.findByUsername(USERNAME)).willReturn(user)

            // when
            val isAdmin = testee.isAdmin()

            // then
            assertThat(isAdmin).isFalse()
        }

        @Test
        fun `should be false when there is no authentication`() {
            // given

            // when
            val isAdmin = testee.isAdmin()

            // then
            assertThat(isAdmin).isFalse()
            then(userRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should be false when the authenticated user is unknown`() {
            // given
            authenticateAs(USERNAME)
            given(userRepository.findByUsername(USERNAME)).willReturn(null)

            // when
            val isAdmin = testee.isAdmin()

            // then
            assertThat(isAdmin).isFalse()
        }
    }

    @Nested
    inner class IsStaff {

        @Test
        fun `should be true when the current user has a staff role`() {
            // given
            authenticateAs(USERNAME)
            val user = user().username(USERNAME).build()
            userRole().user(user).role(staffRole(branch().build()).build()).build()
            given(userRepository.findByUsername(USERNAME)).willReturn(user)

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isTrue()
        }

        @Test
        fun `should be true when the current user is admin`() {
            // given
            authenticateAs(USERNAME)
            val user = user().username(USERNAME).build()
            userRole().user(user).role(adminRole().build()).build()
            given(userRepository.findByUsername(USERNAME)).willReturn(user)

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isTrue()
        }

        @Test
        fun `should be false when the current user only has a member role`() {
            // given
            authenticateAs(USERNAME)
            val user = user().username(USERNAME).build()
            userRole().user(user).role(memberRole(branch().build()).build()).build()
            given(userRepository.findByUsername(USERNAME)).willReturn(user)

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isFalse()
        }

        @Test
        fun `should be false when the current user has no roles`() {
            // given
            authenticateAs(USERNAME)
            given(userRepository.findByUsername(USERNAME)).willReturn(user().username(USERNAME).build())

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isFalse()
        }
    }

    companion object {
        private const val USERNAME = "username"
    }
}
