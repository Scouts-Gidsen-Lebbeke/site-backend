package be.sgl.backend.service.user

import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.security.UserLevelProvider
import org.springframework.stereotype.Component

@Component
class RepositoryUserLevelProvider(private val userRepository: UserRepository) : UserLevelProvider {

    override fun levelOf(username: String): RoleLevel? {
        return userRepository.findByUsername(username)?.level
    }
}
