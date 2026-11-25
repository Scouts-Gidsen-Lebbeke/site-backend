package be.sgl.backend.organization.application

import be.sgl.backend.organization.RoleDeleting
import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.Role.Companion.memberRole
import be.sgl.backend.organization.domain.Role.Companion.staffRole
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.web.MemberRoleChangeRequest
import be.sgl.backend.organization.web.RoleDTO
import be.sgl.backend.organization.web.StaffRoleChangeRequest
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.shared.exception.RoleNotFoundException
import be.sgl.backend.shared.security.RoleLevel
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// If the external function references get updated by whatever CRUD operation,
// a sync will create the new external functions and remove the old ones afterward.
@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val branchRepository: BranchRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    fun getAllRoles(): List<RoleDTO> {
        return roleRepository.findAll().map(RoleDTO::of)
    }

    fun getAdminRole(): RoleDTO {
        return RoleDTO.of(findAdminRole())
    }

    fun createMemberRole(branchId: Int, dto: MemberRoleChangeRequest): RoleDTO {
        val branch = getBranchById(branchId)
        val newRole = memberRole(dto.name!!, dto.externalId!!, dto.backupExternalId, branch)
        return RoleDTO.of(roleRepository.save(newRole))
    }

    fun createStaffRole(branchId: Int, dto: StaffRoleChangeRequest): RoleDTO {
        val branch = getBranchById(branchId)
        val newRole = staffRole(dto.name!!, dto.externalId, dto.backupExternalId, branch, dto.staffLevel)
        return RoleDTO.of(roleRepository.save(newRole))
    }

    fun updateMemberRole(id: Int, dto: MemberRoleChangeRequest): RoleDTO {
        val role = getRoleById(id)
        check(role.memberRole) { "The requested role to update is not a member role!" }
        role.name = dto.name!!
        role.externalId = dto.externalId
        role.backupExternalId = dto.backupExternalId
        return RoleDTO.of(roleRepository.save(role))
    }

    fun updateStaffRole(id: Int, dto: StaffRoleChangeRequest): RoleDTO {
        val role = getRoleById(id)
        check(role.staffRole) { "The requested role to update is not a staff role!" }
        dto.name?.let { role.name = it }
        role.externalId = dto.externalId
        role.backupExternalId = dto.backupExternalId
        role.level = if (dto.staffLevel) RoleLevel.STAFF else RoleLevel.GUEST
        return RoleDTO.of(roleRepository.save(role))
    }

    @Transactional
    fun deleteRole(id: Int) {
        val role = getRoleById(id)
        check(!role.adminRole) { "Admin roles can't be deleted!" }
        eventPublisher.publishEvent(RoleDeleting(id))
        roleRepository.delete(role)
    }

    fun getRoleToSyncByBranch(branchId: Int): RoleDTO? {
        val branch = getBranchById(branchId)
        return roleRepository.getRoleToSyncByBranch(branch)?.let(RoleDTO::of)
    }

    fun getStaffRoleToSyncByBranch(branchId: Int): RoleDTO? {
        val branch = getBranchById(branchId)
        return roleRepository.getStaffRoleToSyncByBranch(branch)?.let(RoleDTO::of)
    }

    fun findAdminRole(): Role {
        return roleRepository.findByLevel(RoleLevel.ADMIN).firstOrNull()
            ?: throw IllegalStateException("No admin role configured!")
    }

    fun getRoleById(id: Int): Role {
        return roleRepository.findById(id).orElseThrow { RoleNotFoundException() }
    }

    fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}
