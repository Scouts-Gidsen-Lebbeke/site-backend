package be.sgl.backend.people.application

import be.sgl.backend.people.web.MedicalRecordDTO
import be.sgl.backend.people.domain.MedicalRecordRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.exception.UserNotFoundException
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