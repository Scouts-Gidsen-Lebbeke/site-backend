package be.sgl.backend.dto

import be.sgl.backend.entity.user.RoleLevel
import jakarta.validation.constraints.NotBlank

data class RoleDTO(
    val id: Int?,
    val externalId: String?,
    val backupExternalId: String?,
    @NotBlank
    val name: String,
    val branch: BranchBaseDTO?,
    val staffBranch: BranchBaseDTO?,
    val level: RoleLevel
)

data class ExternalFunction(val externalId: String, val name: String, val paid: Boolean)