package be.sgl.backend.organization

import be.sgl.backend.shared.security.RoleLevel

data class RoleSummary(
    val id: Int,
    val name: String,
    val level: RoleLevel,
    val branchId: Int?,
    val staffBranchId: Int?,
    val externalId: String?,
    val backupExternalId: String?
) {
    val forExternalSync: Boolean
        get() = branchId != null
    val memberRole: Boolean
        get() = branchId != null && staffBranchId == null && level == RoleLevel.GUEST
    val staffRole: Boolean
        get() = staffBranchId != null && branchId == null
    val adminRole: Boolean
        get() = level == RoleLevel.ADMIN
}
