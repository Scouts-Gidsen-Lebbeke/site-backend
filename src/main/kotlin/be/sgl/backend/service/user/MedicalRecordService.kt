package be.sgl.backend.service.user

import be.sgl.backend.dto.user.MedicalRecordDTO
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.exception.UserNotFoundException
import be.sgl.backend.mapper.user.MedicalRecordMapper
import org.springframework.stereotype.Service

@Service
class MedicalRecordService(
    private val userRepository: UserRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val mapper: MedicalRecordMapper
) {

    fun getMedicalRecord(username: String): MedicalRecordDTO? {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        return medicalRecordRepository.findMedicalRecordByUser(user)?.run(mapper::toDto)
    }
}