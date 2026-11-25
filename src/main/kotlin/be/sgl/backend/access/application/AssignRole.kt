package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRole
import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.shared.util.Usecase

@Usecase
class AssignRole(private val userRoleRepository: UserRoleRepository) {

    fun execute(userId: Int, roleId: Int): Boolean {
        if (userRoleRepository.existsById(UserRole.Key(userId, roleId))) {
            return false
        }
        userRoleRepository.save(UserRole(userId, roleId))
        return true
    }
}
