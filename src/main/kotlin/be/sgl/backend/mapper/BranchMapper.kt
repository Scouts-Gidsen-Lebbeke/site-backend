package be.sgl.backend.mapper

import be.sgl.backend.dto.BranchBaseDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.dto.StaffDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface BranchMapper {
    fun toBaseDto(branch: Branch): BranchBaseDTO
    fun toEntity(dto: BranchBaseDTO): Branch
    @Mapping(target = "staff", expression = "java(BranchMapperKt.asStaffDTO(branch, branch.getStaff()))")
    fun toDto(branch: Branch): BranchDTO
    fun toEntity(dto: BranchDTO): Branch
}

fun asStaffDTO(branch: Branch, staff: List<User>): List<StaffDTO> {
    return staff.map { StaffDTO(it.name, it.firstName, it.image, it.staffData.nicknames[branch], it.staffData.totem) }
}