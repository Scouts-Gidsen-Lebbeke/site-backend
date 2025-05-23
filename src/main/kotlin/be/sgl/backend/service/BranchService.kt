package be.sgl.backend.service

import be.sgl.backend.dto.BranchBaseDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.mapper.BranchMapper
import org.springframework.beans.factory.annotation.Autowired
import be.sgl.backend.service.ImageService.ImageDirectory.*
import org.springframework.stereotype.Service

@Service
class BranchService {

    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var mapper: BranchMapper
    @Autowired
    private lateinit var imageService: ImageService

    fun getBranchDTOById(id: Int): BranchDTO {
        val branch = getBranchById(id)
        branch.staff = userRepository.getStaffForBranch(branch)
        return mapper.toDto(branch)
    }

    fun getBranchesWithCalendar(): List<BranchBaseDTO> {
        return branchRepository.getBranchesWithCalendar().map(mapper::toBaseDto)
    }

    fun getVisibleBranches(): List<BranchBaseDTO> {
        return branchRepository.getVisibleBranches().map(mapper::toBaseDto)
    }

    fun getAllBranches(): List<BranchDTO> {
        return branchRepository.findAll().map(mapper::toDto)
    }

    fun saveBranchDTO(dto: BranchDTO): BranchDTO {
        val newBranch = mapper.toEntity(dto)
        imageService.move(newBranch.image, TEMPORARY, BRANCH)
        return mapper.toDto(branchRepository.save(newBranch))
    }

    fun mergeBranchDTOChanges(id: Int, dto: BranchDTO): BranchDTO {
        val branch = getBranchById(id)
        branch.name = dto.name
        branch.email = dto.email
        branch.minimumAge = dto.minimumAge
        branch.maximumAge = dto.maximumAge
        branch.sex = dto.sex
        branch.description = dto.description
        branch.law = dto.law
        if (branch.image != dto.image) {
            imageService.delete(BRANCH, branch.image)
            imageService.move(dto.image, TEMPORARY, BRANCH)
            branch.image = dto.image
        }
        branch.status = dto.status
        branch.staffTitle = dto.staffTitle
        return mapper.toDto(branchRepository.save(branch))
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}