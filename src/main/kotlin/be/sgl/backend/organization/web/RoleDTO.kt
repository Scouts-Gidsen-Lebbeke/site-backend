package be.sgl.backend.organization.web

import be.sgl.backend.organization.domain.Role
import be.sgl.backend.shared.security.RoleLevel
import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "Role")
data class RoleDTO(
    val id: Int,
    val externalId: String?,
    val backupExternalId: String?,
    val name: String,
    val branch: BranchBaseDTO?,
    val staffBranch: BranchBaseDTO?,
    val level: RoleLevel
) {
    companion object {
        fun of(role: Role): RoleDTO {
            return RoleDTO(role.id!!, role.externalId, role.backupExternalId, role.name,
                role.branch?.let(BranchBaseDTO::of), role.staffBranch?.let(BranchBaseDTO::of), role.level)
        }
    }
}
