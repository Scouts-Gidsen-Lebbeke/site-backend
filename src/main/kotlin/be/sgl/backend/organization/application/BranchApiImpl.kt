package be.sgl.backend.organization.application

import be.sgl.backend.organization.BranchApi
import be.sgl.backend.organization.BranchSummary
import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class BranchApiImpl(private val branchRepository: BranchRepository) : BranchApi {

    override fun isActive(branchId: Int): Boolean {
        return branchRepository.findByIdOrNull(branchId)?.status == BranchStatus.ACTIVE
    }

    override fun findSummaries(ids: Collection<Int>): Map<Int, BranchSummary> {
        return branchRepository.findAllById(ids).map(::toSummary).associateBy(BranchSummary::id)
    }

    override fun findBranchesWithCalendar(): List<BranchSummary> {
        return branchRepository.getBranchesWithCalendar().map(::toSummary)
    }

    private fun toSummary(branch: Branch) = BranchSummary(branch.id!!, branch.name, branch.image)
}
