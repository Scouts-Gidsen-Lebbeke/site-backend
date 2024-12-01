package be.sgl.backend.service.mapper

import be.sgl.backend.dto.BranchBaseDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.entity.branch.Branch
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface BranchMapper {
    fun toBaseDto(branch: Branch): BranchBaseDTO
    fun toEntity(dto: BranchBaseDTO): Branch
    fun toDto(branch: Branch): BranchDTO
    fun toEntity(dto: BranchDTO): Branch
}