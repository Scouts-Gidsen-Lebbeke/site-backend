package be.sgl.backend.shared.security

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
    lateinit var userLevelProvider: UserLevelProvider
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
        fun `should be true when the current user has admin level`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(RoleLevel.ADMIN)

            // when
            val isAdmin = testee.isAdmin()

            // then
            assertThat(isAdmin).isTrue()
        }

        @Test
        fun `should be false when the current user has staff level`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(RoleLevel.STAFF)

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
            then(userLevelProvider).shouldHaveNoInteractions()
        }

        @Test
        fun `should be false when the authenticated user is unknown`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(null)

            // when
            val isAdmin = testee.isAdmin()

            // then
            assertThat(isAdmin).isFalse()
        }
    }

    @Nested
    inner class IsStaff {

        @Test
        fun `should be true when the current user has staff level`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(RoleLevel.STAFF)

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isTrue()
        }

        @Test
        fun `should be true when the current user is admin`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(RoleLevel.ADMIN)

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isTrue()
        }

        @Test
        fun `should be false when the current user only has guest level`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(RoleLevel.GUEST)

            // when
            val isStaff = testee.isStaff()

            // then
            assertThat(isStaff).isFalse()
        }

        @Test
        fun `should be false when the current user has no level`() {
            // given
            authenticateAs(USERNAME)
            given(userLevelProvider.levelOf(USERNAME)).willReturn(RoleLevel.GUEST)

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
