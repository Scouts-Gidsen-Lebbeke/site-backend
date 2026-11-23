package be.sgl.backend.service.branch

import be.sgl.backend.dto.branch.BranchDTO
import be.sgl.backend.dto.branch.BranchWithStaff
import be.sgl.backend.dto.branch.CreateOrUpdateBranchRequest
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.exception.BranchNotFoundException
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.service.ImageService.ImageDirectory.BRANCH
import be.sgl.backend.service.SettingService
import org.springframework.stereotype.Service

@Service
class BranchService(
    private val branchRepository: BranchRepository,
    private val userRepository: UserRepository,
    private val imageService: ImageService,
    private val settingService: SettingService
) {

    fun getBranchDTOById(id: Int): BranchWithStaff {
        val branch = getBranchById(id)
        if (settingService.getOrDefault(SettingId.STAFF_VISIBLE.name, true)) {
            branch.staff = userRepository.getStaffForBranch(branch)
        }
        return BranchWithStaff.of(branch)
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