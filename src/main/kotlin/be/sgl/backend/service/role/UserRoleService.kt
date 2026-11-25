package be.sgl.backend.service.role

import be.sgl.backend.access.AccessApi
import be.sgl.backend.dto.role.UserRoleDTO
import be.sgl.backend.organization.application.BranchService
import be.sgl.backend.organization.application.RoleService
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.web.BranchDTO
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.shared.exception.UserNotFoundException
import org.springframework.stereotype.Service

@Service
class UserRoleService(
    private val roleService: RoleService,
    private val roleRepository: RoleRepository,
    private val branchService: BranchService,
    private val userDataProvider: UserDataProvider,
    private val accessApi: AccessApi,
    private val userRepository: UserRepository
) {

    fun markUserAsStaff(branchId: Int, username: String): UserRoleDTO {
        val role = roleRepository.getStaffRoleToSyncByBranch(roleService.getBranchById(branchId))
        checkNotNull(role) { "No staff role configured for the given branch!" }
        return assignRole(username, role)
    }

    fun markUserAsAdmin(username: String): UserRoleDTO {
        val role = roleService.findAdminRole()
        return assignRole(username, role)
    }

    private fun assignRole(username: String, role: Role): UserRoleDTO {
        val user = getUserByUsername(username)
        check(userDataProvider.startRole(user, role)) { "User already has this role!" }
        return UserRoleDTO.of(user, role)
    }

    fun deassignRoleFromUser(roleId: Int, username: String) {
        val user = getUserByUsername(username)
        val role = roleService.getRoleById(roleId)
        userDataProvider.endRole(user, role)
    }

    fun getUserRolesByRole(roleId: Int): List<UserRoleDTO> {
        val role = roleService.getRoleById(roleId)
        return userRepository.findAllById(accessApi.userIdsWithRole(roleId)).map { UserRoleDTO.of(it, role) }
    }

    fun getStaffBranch(username: String): BranchDTO? {
        val user = getUserByUsername(username)
        return accessApi.staffBranchIdOf(user.id!!)?.let(branchService::getBranch)
    }

    private fun getUserByUsername(username: String) =
        userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
}
