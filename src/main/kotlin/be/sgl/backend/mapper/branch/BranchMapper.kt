package be.sgl.backend.mapper.branch

import be.sgl.backend.dto.branch.BranchDTO
import be.sgl.backend.dto.branch.BranchWithStaff
import be.sgl.backend.dto.branch.CreateOrUpdateBranchRequest
import be.sgl.backend.dto.user.StaffDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface BranchMapper {
    fun toDto(branch: Branch): BranchDTO
    @Mapping(target = "staff", expression = "java(BranchMapperKt.asStaffDTO(branch, branch.getStaff()))")
    fun toBranchWithStaff(branch: Branch): BranchWithStaff
    fun toEntity(request: CreateOrUpdateBranchRequest): Branch
}

fun asStaffDTO(branch: Branch, staff: List<User>): List<StaffDTO> {
    return staff.map { StaffDTO(it.name, it.firstName, it.image, it.staffData.nicknames[branch], it.staffData.totem) }
}