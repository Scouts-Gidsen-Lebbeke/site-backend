package be.sgl.backend.dto.role

import be.sgl.backend.dto.branch.BranchBaseDTO
import be.sgl.backend.entity.user.RoleLevel
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
)