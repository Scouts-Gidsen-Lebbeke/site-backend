package be.sgl.backend.organization.application

import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.web.BranchDTO
import be.sgl.backend.organization.web.CreateOrUpdateBranchRequest
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.shared.image.ImageService.ImageDirectory.BRANCH
import org.springframework.stereotype.Service

@Service
class BranchService(
    private val branchRepository: BranchRepository,
    private val imageService: ImageService
) {

    fun getBranch(id: Int): BranchDTO {
        return BranchDTO.of(getBranchById(id))
    }

    fun getBranchesWithCalendar(): List<BranchDTO> {
        return branchRepository.getBranchesWithCalendar().map(BranchDTO::of)
    }

    fun getVisibleBranches(): List<BranchDTO> {
        return branchRepository.getVisibleBranches().map(BranchDTO::of)
    }

    fun getAllBranches(): List<BranchDTO> {
        return branchRepository.findAll().map(BranchDTO::of)
    }

    fun createBranch(dto: CreateOrUpdateBranchRequest): BranchDTO {
        val newBranch = dto.toEntity()
        imageService.move(newBranch.image, TEMPORARY, BRANCH)
        return BranchDTO.of(branchRepository.save(newBranch))
    }

    fun updateBranch(id: Int, request: CreateOrUpdateBranchRequest): BranchDTO {
        val branch = getBranchById(id)
        val branchFromDto = request.toEntity()
        branch.name = branchFromDto.name
        branch.email = branchFromDto.email
        branch.minimumAge = branchFromDto.minimumAge
        branch.maximumAge = branchFromDto.maximumAge
        branch.sex = branchFromDto.sex
        branch.description = branchFromDto.description
        branch.law = branchFromDto.law
        if (branch.image != branchFromDto.image) {
            imageService.delete(BRANCH, branch.image)
            imageService.move(branchFromDto.image, TEMPORARY, BRANCH)
            branch.image = branchFromDto.image
        }
        branch.status = branchFromDto.status
        branch.staffTitle = branchFromDto.staffTitle
        return BranchDTO.of(branchRepository.save(branch))
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}
