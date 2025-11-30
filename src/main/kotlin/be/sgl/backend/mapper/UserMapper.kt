package be.sgl.backend.mapper

import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.dto.user.MedicalRecordDTO
import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.User
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface UserMapper {
    fun toDto(user: User): UserDTO
    fun toEntity(dto: UserDTO): User
    fun toDto(branch: Branch): BranchDTO
    fun toDto(medicalRecord: MedicalRecord): MedicalRecordDTO
}