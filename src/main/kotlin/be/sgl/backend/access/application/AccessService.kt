package be.sgl.backend.access.application

import be.sgl.backend.access.AccessApi
import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.RoleApi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccessService(
    private val userRoleRepository: UserRoleRepository,
    private val roleApi: RoleApi,
    private val assignRole: AssignRole,
    private val revokeRole: RevokeRole,
    private val findStaffBranchForUser: FindStaffBranchForUser
) : AccessApi {

    override fun roleIdsOf(userId: Int): List<Int> {
        return userRoleRepository.findByUserId(userId).map { it.roleId }
    }

    override fun userIdsWithRole(roleId: Int): List<Int> {
        return userRoleRepository.findByRoleId(roleId).map { it.userId }
    }

    override fun staffUserIdsOf(branchId: Int): List<Int> {
        val staffRoleIds = roleApi.findStaffRoles(branchId).map { it.id }
        if (staffRoleIds.isEmpty()) {
            return emptyList()
        }
        return userRoleRepository.findByRoleIdIn(staffRoleIds).map { it.userId }.distinct()
    }

    override fun staffBranchIdOf(userId: Int): Int? {
        return findStaffBranchForUser.execute(userId)
    }

    @Transactional
    override fun assign(userId: Int, roleId: Int): Boolean {
        return assignRole.execute(userId, roleId)
    }

    @Transactional
    override fun revoke(userId: Int, roleId: Int) {
        revokeRole.execute(userId, roleId)
    }
}
