package be.sgl.backend.service.role

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.shared.util.Usecase

@Usecase
class FindStaffBranchForUser(private val userRepository: UserRepository) {

    fun execute(username: String): Branch? {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        return user.roles
            .mapNotNull { it.role.staffBranch }
            .firstOrNull { it.status == BranchStatus.ACTIVE }
    }
}
