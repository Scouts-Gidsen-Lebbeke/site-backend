package be.sgl.backend.service.user.inital

import be.sgl.backend.shared.config.CustomUserDetails
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.Role.Companion.adminRole
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.entity.role.UserRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.util.ForInternalOrganization
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@ForInternalOrganization
class CheckForInitialRunInternal(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
) : CheckForInitialRun {

    private var isInitialRun = false

    @PostConstruct
    fun checkInitialRun() {
        isInitialRun = userRepository.count() == 0L
    }

    override fun execute(userDetails: CustomUserDetails): User? {
        if (!isInitialRun) return null
        val adminRole = createAdminRoleIfNeeded()
        var user = User().apply {
            name = userDetails.lastName
            firstName = userDetails.firstName
            email = userDetails.email
            username = userDetails.username
            birthdate = LocalDate.now() // non-null
        }
        user = userRepository.save(user)
        // the first user administers the app, the role can only be linked once the user has an id
        adminRole?.let {
            user.roles.add(UserRole(user, it))
            user = userRepository.save(user)
        }
        isInitialRun = false
        return user
    }

    /**
     * Saves a new admin role when no roles exist yet, otherwise returns the existing admin role (if any).
     */
    fun createAdminRoleIfNeeded(): Role? {
        if (roleRepository.count() == 0L) {
            return adminRole("Admin", null, null).also(roleRepository::save)
        }
        return roleRepository.findByLevel(RoleLevel.ADMIN).firstOrNull()
    }
}