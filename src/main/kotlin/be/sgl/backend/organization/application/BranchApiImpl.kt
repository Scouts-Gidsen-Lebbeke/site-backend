package be.sgl.backend.organization.application

import be.sgl.backend.organization.BranchApi
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class BranchApiImpl(private val branchRepository: BranchRepository) : BranchApi {

    override fun isActive(branchId: Int): Boolean {
        return branchRepository.findByIdOrNull(branchId)?.status == BranchStatus.ACTIVE
    }
}
