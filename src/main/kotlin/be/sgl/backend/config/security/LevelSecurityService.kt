package be.sgl.backend.config.security

import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.service.user.UserDataProvider
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
        // TODO: if a user without a known (but valid) login logs in, we get NPEs all over the place
        val user = userDataProvider.findUser(authentication?.name ?: return false) ?: return false
        return user.level >= level
    }
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@levelSecurityService.isAdmin()")
@ApiResponse(responseCode = "403", description = "User has no admin role")
annotation class OnlyAdmin

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("@levelSecurityService.isStaff()")
@ApiResponse(responseCode = "403", description = "User has no staff role")
annotation class OnlyStaff

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
@ApiResponse(responseCode = "403", description = "User is not logged in")
annotation class OnlyAuthenticated

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("permitAll()")
annotation class Public