package be.sgl.backend.dto

import be.sgl.backend.entity.user.RoleLevel
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

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

data class MemberRoleDTO(
    @NotBlank
    var externalId: String?,
    var backupExternalId: String?,
    @NotBlank
    var name: String?,
)

data class StaffRoleDTO(
    var externalId: String?,
    var backupExternalId: String?,
    @NotBlank
    var name: String?,
    var staffLevel: Boolean
)

data class UserRoleDTO(
    var id: Int?,
    var user: UserDTO,
    var role: RoleDTO,
    var startDate: LocalDate?,
    var endDate: LocalDate?
)

data class StaffLinkDTO(
    @NotNull
    var username: String?,
    @NotNull
    var branchId: Int?
)

data class ExternalFunction(val externalId: String, val name: String, val paid: Boolean)