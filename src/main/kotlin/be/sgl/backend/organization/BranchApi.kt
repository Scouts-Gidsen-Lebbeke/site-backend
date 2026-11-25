package be.sgl.backend.organization

interface BranchApi {
    fun isActive(branchId: Int): Boolean
}
