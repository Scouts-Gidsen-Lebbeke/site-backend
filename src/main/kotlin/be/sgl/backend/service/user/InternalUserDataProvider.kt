package be.sgl.backend.service.user

import be.sgl.backend.entity.user.User
import be.sgl.backend.util.ForInternalOrganization
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * The 'easy' way of managing user data. Everything is kept internally.
 */
@Service
@ForInternalOrganization
class InternalUserDataProvider : UserDataProvider() {

    private val logger = KotlinLogging.logger {}

    override fun acceptRegistration(user: User) {
        logger.debug { "Accepting registration for user ${user.id}..." }
        check(user.username == null) { "Registration acceptance should not be performed on already known users!" }
        // TODO: This is completely wrong. This should trigger an account creation request in the auth provider instead
        user.username = "${user.name}.${user.firstName}"
        userRepository.save(user)
        logger.debug { "Internal registration finished: username ${user.username} created for user ${user.id}!" }
    }
}