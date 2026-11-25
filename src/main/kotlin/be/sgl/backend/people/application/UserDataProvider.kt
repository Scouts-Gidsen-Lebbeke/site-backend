package be.sgl.backend.people.application

import be.sgl.backend.people.domain.User
import be.sgl.backend.people.domain.UserRepository
import mu.KotlinLogging

abstract class UserDataProvider(protected val userRepository: UserRepository) {

    private val logger = KotlinLogging.logger {}

    abstract fun acceptRegistration(user: User)

    open fun updateUser(user: User): User {
        logger.debug { "Updating user data for ${user.username}..." }
        return userRepository.save(user)
    }
}
