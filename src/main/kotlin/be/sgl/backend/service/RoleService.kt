package be.sgl.backend.service

import be.sgl.backend.dto.*
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.Role.Companion.memberRole
import be.sgl.backend.entity.user.Role.Companion.staffRole
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.mapper.RoleMapper
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.UserRoleRepository
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.service.exception.RoleNotFoundException
import be.sgl.backend.service.exception.UserRoleNotFoundException
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RoleService {

    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var mapper: RoleMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    fun getAllRoles(): List<RoleDTO> {
        return roleRepository.findAll().map(mapper::toDto)
    }

    fun getAdminRole(): RoleDTO {
        return mapper.toDto(findAdminRole())
    }

    fun createMemberRole(branchId: Int, dto: MemberRoleDTO): RoleDTO {
        val branch = getBranchById(branchId)
        val newRole = memberRole(dto.name!!, dto.externalId!!, dto.backupExternalId, branch)
        return mapper.toDto(roleRepository.save(newRole))
    }

    fun createStaffRole(branchId: Int, dto: StaffRoleDTO): RoleDTO {
        val branch = getBranchById(branchId)
        val newRole = staffRole(dto.name!!, dto.externalId, dto.backupExternalId, branch, dto.staffLevel)
        return mapper.toDto(roleRepository.save(newRole))
    }

    fun mergeMemberRoleDTOChanges(id: Int, dto: MemberRoleDTO): RoleDTO {
        val role = getRoleById(id)
        check(role.memberRole) { "The requested role to update is not a member role!" }
        role.name = dto.name!!
        // TODO: this implies looping over the active memberships and assigning additional user roles (synced with external functions afterwards)
        role.externalId = dto.externalId
        role.backupExternalId = dto.backupExternalId
        return mapper.toDto(roleRepository.save(role))
    }

    fun mergeStaffRoleDTOChanges(id: Int, dto: StaffRoleDTO): RoleDTO {
        val role = getRoleById(id)
        check(role.staffRole) { "The requested role to update is not a staff role!" }
        role.name = dto.name!!
        // If the external function references get updated, a sync afterward will create the new external functions.
        // TODO: consider removing external functions not linked to an internal role in the sync
        role.externalId = dto.externalId
        role.backupExternalId = dto.backupExternalId
        role.level = if (dto.staffLevel) RoleLevel.STAFF else RoleLevel.GUEST
        return mapper.toDto(roleRepository.save(role))
    }

    fun deleteRole(id: Int) {
        val role = getRoleById(id)
        check(role.level != RoleLevel.ADMIN) { "Admin roles can't be deleted!" }
        // TODO: see above
        userRoleRepository.deleteUserRolesByRole(role)
        roleRepository.delete(role)
    }

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
        val user = userDataProvider.getUser(username)
        val newRole = userDataProvider.startRole(user, role)
        checkNotNull(newRole) { "User already has this role!" }
        return mapper.toDto(newRole)
    }

    fun deassignRoleFromUser(id: Int) {
        val userRole = userRoleRepository.findById(id).orElseThrow { UserRoleNotFoundException() }
        check(!userRole.role.memberRole) { "Member roles can't be manually deassigned!" }
        if (userRole.role.adminRole) {
            check(userRoleRepository.findByRole(userRole.role).isNotEmpty()) { "At least one admin should exist!" }
        }
        userRoleRepository.delete(userRole)
    }

    fun getAllExternalFunctions(): List<ExternalFunction> {
        return organizationProvider.getAllExternalFunctions()
    }

    fun getPaidExternalFunctions(): List<ExternalFunction> {
        return organizationProvider.getPaidExternalFunctions()
    }

    fun getRoleToSyncByBranch(branchId: Int): RoleDTO? {
        val branch = getBranchById(branchId)
        return roleRepository.getRoleToSyncByBranch(branch)?.let(mapper::toDto)
    }

    fun getStaffRoleToSyncByBranch(branchId: Int): RoleDTO? {
        val branch = getBranchById(branchId)
        return roleRepository.getStaffRoleToSyncByBranch(branch)?.let(mapper::toDto)
    }

    fun getUserRolesByRole(roleId: Int): List<UserRoleDTO> {
        val role = getRoleById(roleId)
        return userRoleRepository.findByRole(role).map(mapper::toDto)
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