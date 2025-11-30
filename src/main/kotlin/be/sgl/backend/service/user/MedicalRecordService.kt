package be.sgl.backend.service.user

import be.sgl.backend.dto.user.MedicalRecordDTO
import be.sgl.backend.mapper.UserMapper
import be.sgl.backend.repository.user.MedicalRecordRepository
import org.springframework.stereotype.Service

@Service
class MedicalRecordService(
    private val userDataProvider: UserDataProvider,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val mapper: UserMapper
) {

    fun getMedicalRecord(username: String): MedicalRecordDTO? {
        val user = userDataProvider.getUser(username)
        return medicalRecordRepository.getMedicalRecordByUser(user)?.run(mapper::toDto)
    }
}