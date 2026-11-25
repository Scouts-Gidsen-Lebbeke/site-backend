package be.sgl.backend.access

interface AccessApi {
    fun roleIdsOf(userId: Int): List<Int>
    fun userIdsWithRole(roleId: Int): List<Int>
    fun staffUserIdsOf(branchId: Int): List<Int>
    fun staffBranchIdOf(userId: Int): Int?
    fun assign(userId: Int, roleId: Int): Boolean
    fun revoke(userId: Int, roleId: Int)
}
