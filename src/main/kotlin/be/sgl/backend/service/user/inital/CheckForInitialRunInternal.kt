package be.sgl.backend.service.user.inital

import be.sgl.backend.access.AccessApi
import be.sgl.backend.shared.config.CustomUserDetails
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.Role.Companion.adminRole
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.people.application.CheckForInitialRun
import be.sgl.backend.people.domain.User
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.util.ForInternalOrganization
import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
@ForInternalOrganization
class CheckForInitialRunInternal(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val accessApi: AccessApi,
    private val entityManager: EntityManager
) : CheckForInitialRun {

    private var isInitialRun = false

    @PostConstruct
    fun checkInitialRun() {
        isInitialRun = userRepository.count() == 0L
    }

    @Transactional
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
            accessApi.assign(user.id!!, it.id!!)
            entityManager.flush()
            entityManager.refresh(user)
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