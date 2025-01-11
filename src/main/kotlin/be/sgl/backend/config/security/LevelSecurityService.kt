package be.sgl.backend.config.security

import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.service.user.UserDataProvider
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class LevelSecurityService {

    @Autowired
    private lateinit var userDataProvider: UserDataProvider

    fun isAdmin() = currentUserHasRoleWithLevel(RoleLevel.ADMIN)

    fun isStaff() = currentUserHasRoleWithLevel(RoleLevel.STAFF)

    private fun currentUserHasRoleWithLevel(level: RoleLevel): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = userDataProvider.getUser(authentication?.name ?: return false)
        return user.roles.any { it.role.level == level }
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@levelSecurityService.isAdmin()")
annotation class OnlyAdmin

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@levelSecurityService.isStaff()")
annotation class OnlyStaff

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
annotation class OnlyAuthenticated
