package be.sgl.backend.service.user

import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.security.RoleLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class RepositoryUserLevelProviderTest {

    @Mock
    lateinit var userRepository: UserRepository
    @InjectMocks
    lateinit var testee: RepositoryUserLevelProvider

    private val username = "jdoe"

    @Test
    fun `should return the highest role level when the user has several roles`() {
        // given
        val user = user().username(username).build()
        userRole().user(user).role(staffRole(branch().build()).build()).build()
        userRole().user(user).role(adminRole().build()).build()
        given(userRepository.findByUsername(username)).willReturn(user)

        // when
        val level = testee.levelOf(username)

        // then
        assertThat(level).isEqualTo(RoleLevel.ADMIN)
    }

    @Test
    fun `should return guest when the user has no roles`() {
        // given
        given(userRepository.findByUsername(username)).willReturn(user().username(username).build())

        // when
        val level = testee.levelOf(username)

        // then
        assertThat(level).isEqualTo(RoleLevel.GUEST)
    }

    @Test
    fun `should return no level when the user is unknown`() {
        // given
        given(userRepository.findByUsername(username)).willReturn(null)

        // when
        val level = testee.levelOf(username)

        // then
        assertThat(level).isNull()
    }
}
