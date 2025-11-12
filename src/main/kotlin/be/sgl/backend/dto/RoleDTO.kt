package be.sgl.backend.dto

import be.sgl.backend.entity.user.RoleLevel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// read-only
data class RoleDTO(
    val id: Int?,
    val externalId: String?,
    val backupExternalId: String?,
    val name: String,
    val branch: BranchBaseDTO?,
    val staffBranch: BranchBaseDTO?,
    val level: RoleLevel
)

data class MemberRoleDTO(
    @field:NotBlank
    var externalId: String?,
    var backupExternalId: String?,
    @field:NotBlank
    var name: String?,
)

data class StaffRoleDTO(
    var externalId: String?,
    var backupExternalId: String?,
    @field:NotBlank
    var name: String?,
    var staffLevel: Boolean = false
)

// read-only
data class UserRoleDTO(
    var id: Int?,
    var user: UserDTO,
    var role: RoleDTO
)

data class StaffLinkDTO(
    @field:NotNull
    var username: String?,
    @field:NotNull
    var branchId: Int?
)

// read-only
data class ExternalFunction(val externalId: String, val name: String, val paid: Boolean)