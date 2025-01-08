package be.sgl.backend.repository

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.branch.BranchStatus.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BranchRepository : JpaRepository<Branch, Int> {
    fun getByStatusIn(vaerstatuses: Collection<BranchStatus>): List<Branch>
    // fun getBranchesWithCalendar() = getByStatusIn(listOf(ACTIVE))
    // fun getPaidBranches() = getByStatusIn(listOf(ACTIVE, MEMBER))
    // fun getVisibleBranches() = getByStatusIn(listOf(ACTIVE, MEMBER, PASSIVE))
}