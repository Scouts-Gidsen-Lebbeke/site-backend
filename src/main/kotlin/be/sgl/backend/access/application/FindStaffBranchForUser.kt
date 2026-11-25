package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.BranchApi
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.shared.util.Usecase

@Usecase
class FindStaffBranchForUser(
    private val userRoleRepository: UserRoleRepository,
    private val roleApi: RoleApi,
    private val branchApi: BranchApi
) {

    fun execute(userId: Int): Int? {
        val roleIds = userRoleRepository.findByUserId(userId).map { it.roleId }
        return roleApi.findRoles(roleIds)
            .mapNotNull { it.staffBranchId }
            .firstOrNull { branchApi.isActive(it) }
    }
}
