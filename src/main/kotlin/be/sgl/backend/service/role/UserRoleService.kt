package be.sgl.backend.service.role

import be.sgl.backend.dto.role.UserRoleDTO
import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.repository.role.UserRoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.shared.exception.RoleNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.shared.exception.UserRoleNotFoundException
import be.sgl.backend.shared.security.RoleLevel
import org.springframework.stereotype.Service

@Service
class UserRoleService(
    private val roleRepository: RoleRepository,
    private val userDataProvider: UserDataProvider,
    private val branchRepository: BranchRepository,
    private val userRoleRepository: UserRoleRepository,
    private val userRepository: UserRepository
) {

    fun markUserAsStaff(branchId: Int, username: String): UserRoleDTO {
        val role = roleRepository.getStaffRoleToSyncByBranch(getBranchById(branchId))
        checkNotNull(role) { "No staff role configured for the given branch!" }
        return assignRole(username, role)
    }

    fun markUserAsAdmin(username: String): UserRoleDTO {
        val role = findAdminRole()
        return assignRole(username, role)
    }

    private fun assignRole(username: String, role: Role): UserRoleDTO {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        val newRole = userDataProvider.startRole(user, role)
        checkNotNull(newRole) { "User already has this role!" }
        return UserRoleDTO.of(newRole)
    }

    fun deassignRoleFromUser(id: Int) {
        val userRole = userRoleRepository.findById(id).orElseThrow { UserRoleNotFoundException() }
        check(!userRole.role.memberRole) { "Member roles can't be manually deassigned!" }
        check(!userRole.role.adminRole || userRoleRepository.findByRole(userRole.role).any { it.user.id != userRole.user.id }) {
            "At least one admin should exist!"
        }
        userDataProvider.endRole(userRole)
    }

    fun getUserRolesByRole(roleId: Int): List<UserRoleDTO> {
        val role = getRoleById(roleId)
        return userRoleRepository.findByRole(role).map(UserRoleDTO::of)
    }

    private fun findAdminRole(): Role {
        return roleRepository.findByLevel(RoleLevel.ADMIN).firstOrNull()
            ?: throw IllegalStateException("No admin role configured!")
    }

    private fun getRoleById(id: Int): Role {
        return roleRepository.findById(id).orElseThrow { RoleNotFoundException() }
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}
