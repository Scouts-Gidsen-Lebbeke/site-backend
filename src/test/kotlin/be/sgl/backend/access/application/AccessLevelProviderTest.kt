package be.sgl.backend.access.application

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummaryTestMother.adminRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.roleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.staffRoleSummary
import be.sgl.backend.people.UserApi
import be.sgl.backend.shared.security.RoleLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AccessLevelProviderTest {

    @Mock
    lateinit var userApi: UserApi
    @Mock
    lateinit var accessApi: AccessApi
    @Mock
    lateinit var roleApi: RoleApi
    @InjectMocks
    lateinit var testee: AccessLevelProvider

    private val username = "jdoe"
    private val userId = 7

    @Test
    fun `should return null when username is unknown`() {
        // given
        given(userApi.findIdByUsername(username)).willReturn(null)

        // when
        val level = testee.levelOf(username)

        // then
        assertThat(level).isNull()
    }

    @Test
    fun `should return guest when user has no roles`() {
        // given
        given(userApi.findIdByUsername(username)).willReturn(userId)
        given(accessApi.roleIdsOf(userId)).willReturn(emptyList())
        given(roleApi.findRoles(emptyList())).willReturn(emptyList())

        // when
        val level = testee.levelOf(username)

        // then
        assertThat(level).isEqualTo(RoleLevel.GUEST)
    }

    @Test
    fun `should return highest level when user has several roles`() {
        // given
        val roles = listOf(roleSummary().id(1).build(), adminRoleSummary().id(2).build(), staffRoleSummary(3).id(3).build())
        given(userApi.findIdByUsername(username)).willReturn(userId)
        given(accessApi.roleIdsOf(userId)).willReturn(roles.map { it.id })
        given(roleApi.findRoles(roles.map { it.id })).willReturn(roles)

        // when
        val level = testee.levelOf(username)

        // then
        assertThat(level).isEqualTo(RoleLevel.ADMIN)
    }
}
