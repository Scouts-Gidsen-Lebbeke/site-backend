package be.sgl.backend.service.user

import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.role.UserRole
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.repository.role.UserRoleRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired

/**
 * Gateway for all user data (contact info, medical data and roles).
 * Split up between an external and an internal way of working.
 */
abstract class UserDataProvider {

    private val logger = KotlinLogging.logger {}

    @Autowired
    protected lateinit var userRepository: UserRepository
    @Autowired
    protected lateinit var userRoleRepository: UserRoleRepository

    /**
     * Mark the given user as completely registered.
     * Should be called when the payment after the initial registration is received.
     */
    abstract fun acceptRegistration(user: User)

    open fun updateUser(user: User): User {
        logger.debug { "Updating user data for ${user.username}..." }
        return userRepository.save(user)
    }

    open fun startRole(user: User, role: Role): UserRole? {
        logger.debug { "Starting role ${role.name} for ${user.username}..." }
        if (user.roles.any { it.role == role }) {
            logger.warn { "${user.username} already has the role ${role.name}! Starting aborted." }
            return null
        }
        val newRole = userRoleRepository.save(UserRole(user, role))
        user.roles.add(newRole)
        return newRole
    }

    open fun endRole(userRole: UserRole) {
        logger.debug { "Ending role ${userRole.role.name} for ${userRole.user.username}..." }
        userRoleRepository.delete(userRole)
    }
}