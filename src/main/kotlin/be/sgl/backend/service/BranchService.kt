package be.sgl.backend.service

import be.sgl.backend.dto.BranchBaseDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus.*
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.mapper.BranchMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BranchService {

    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var mapper: BranchMapper

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

    fun getAllBranches(): List<BranchBaseDTO> {
        return branchRepository.findAll().map(mapper::toBaseDto)
    }

    fun saveBranchDTO(dto: BranchDTO): BranchDTO {
        return mapper.toDto(branchRepository.save(mapper.toEntity(dto)))
    }

    fun mergeBranchDTOChanges(id: Int, dto: BranchDTO): BranchDTO {
        val branch = getBranchById(id)
        branch.name = dto.name
        branch.email = dto.email
        branch.minimumAge = dto.minimumAge
        branch.maximumAge = dto.maximumAge
        branch.description = dto.description
        branch.law = dto.law
        branch.image = dto.image
        branch.staffTitle = dto.staffTitle
        return mapper.toDto(branchRepository.save(branch))
    }

    fun deactivateBranch(id: Int) {
        val branch = getBranchById(id)
        branch.status = PASSIVE
        branchRepository.save(branch)
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}