package be.sgl.backend.organization

interface BranchApi {
    fun isActive(branchId: Int): Boolean
    fun findSummaries(ids: Collection<Int>): Map<Int, BranchSummary>
    fun findBranchesWithCalendar(): List<BranchSummary>
}
