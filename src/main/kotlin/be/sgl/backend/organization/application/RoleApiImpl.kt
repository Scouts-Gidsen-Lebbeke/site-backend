package be.sgl.backend.organization.application

import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummary
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RoleApiImpl(private val roleRepository: RoleRepository) : RoleApi {

    @Transactional(readOnly = true)
    override fun findRoles(ids: Collection<Int>): List<RoleSummary> {
        return roleRepository.findAllById(ids).map { it.toSummary() }
    }

    @Transactional(readOnly = true)
    override fun findStaffRoles(branchId: Int): List<RoleSummary> {
        return roleRepository.findByStaffBranchId(branchId).map { it.toSummary() }
    }

    private fun Role.toSummary(): RoleSummary {
        return RoleSummary(id!!, name, level, branch?.id, staffBranch?.id, externalId, backupExternalId)
    }
}
