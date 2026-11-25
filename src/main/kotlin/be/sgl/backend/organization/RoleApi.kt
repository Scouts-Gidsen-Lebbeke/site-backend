package be.sgl.backend.organization

interface RoleApi {
    fun findRoles(ids: Collection<Int>): List<RoleSummary>
    fun findStaffRoles(branchId: Int): List<RoleSummary>
}
