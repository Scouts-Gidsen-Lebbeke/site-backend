package be.sgl.backend.access.application

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.people.UserApi
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.security.UserLevelProvider
import org.springframework.stereotype.Component

@Component
class AccessLevelProvider(
    private val userApi: UserApi,
    private val accessApi: AccessApi,
    private val roleApi: RoleApi
) : UserLevelProvider {

    override fun levelOf(username: String): RoleLevel? {
        val userId = userApi.findIdByUsername(username) ?: return null
        return roleApi.findRoles(accessApi.roleIdsOf(userId)).maxOfOrNull { it.level } ?: RoleLevel.GUEST
    }
}
