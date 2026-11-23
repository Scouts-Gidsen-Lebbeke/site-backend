package be.sgl.backend.service.user

import be.sgl.backend.dto.user.MedicalRecordDTO
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.exception.UserNotFoundException
import org.springframework.stereotype.Service

@Service
class MedicalRecordService(
    private val userRepository: UserRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
) {

    fun getMedicalRecord(username: String): MedicalRecordDTO? {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        return medicalRecordRepository.findMedicalRecordByUser(user)?.run(MedicalRecordDTO::of)
    }
}