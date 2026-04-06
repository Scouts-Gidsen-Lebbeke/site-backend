package be.sgl.backend.mapper.user

import be.sgl.backend.dto.user.MedicalRecordDTO
import be.sgl.backend.entity.user.MedicalRecord
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface MedicalRecordMapper {
    fun toDto(medicalRecord: MedicalRecord): MedicalRecordDTO
    fun toEntity(dto: MedicalRecordDTO): MedicalRecord
}