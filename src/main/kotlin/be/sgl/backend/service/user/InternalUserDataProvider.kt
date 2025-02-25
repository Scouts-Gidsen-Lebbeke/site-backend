package be.sgl.backend.service.user

import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserRole
import mu.KotlinLogging
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

/**
 * The 'easy' way of managing user data. Everything is kept internally.
 */
@Service
@Conditional(InternalOrganizationCondition::class)
class InternalUserDataProvider : UserDataProvider() {

    private val logger = KotlinLogging.logger {}

    override fun acceptRegistration(user: User) {
        logger.debug { "Accepting registration for user ${user.id}..." }
        check(user.username == null) { "Registration acceptance should not be performed on already known users!" }
        user.username = "${user.name}.${user.firstName}"
        userRepository.save(user)
        logger.debug { "Internal registration finished: username ${user.username} created for user ${user.id}!" }
    }

    override fun findUser(username: String): User? {
        logger.debug { "Fetching user data for $username..." }
        return userRepository.findByUsername(username)
    }

    override fun getUser(username: String): User {
        logger.debug { "Fetching user data for $username..." }
        return userRepository.getByUsername(username)
    }

    override fun findByNameAndEmail(name: String, firstName: String, email: String): User? {
        logger.debug { "Trying to find user with name $firstName $name and email $email..." }
        return userRepository.findByNameAndFirstNameAndEmail(name, firstName, email)
    }

    override fun updateUser(user: User): User {
        logger.debug { "Updating user data for ${user.username}..." }
        return userRepository.save(user)
    }

    override fun startRole(user: User, role: Role) {
        logger.debug { "Starting role ${role.name} for ${user.username}..." }
        if (user.roles.none { it.role == role }) {
            logger.warn { "${user.username} already has the role ${role.name}! Starting aborted." }
            return
        }
        val newRole = userRoleRepository.save(UserRole(user, role))
        user.roles.add(newRole)
    }

    override fun endRole(user: User, role: Role) {
        logger.debug { "Ending role ${role.name} for ${user.username}..." }
        val userRole = user.roles.find { it.role == role }
        if (userRole == null) {
            logger.warn { "${user.username} never had the role ${role.name}! Ending aborted." }
            return
        }
        userRoleRepository.delete(userRole)
        user.roles.remove(userRole)
    }
}