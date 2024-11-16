package be.sgl.backend.service.user

import be.sgl.backend.entity.MedicalRecord
import be.sgl.backend.entity.User
import be.sgl.backend.entity.UserRegistration
import be.sgl.backend.repository.MedicalRecordRepository
import be.sgl.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class InternalUserDataProvider : UserDataProvider {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    override fun createRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun acceptRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun getUser(username: String): User {
        return userRepository.getUserByUsernameEquals(username)
    }

    override fun getUserWithAllData(username: String): User {
        return userRepository.getUserByUsernameEqualsAndUserData(username)
    }

    override fun getMedicalRecord(user: User): MedicalRecord? {
        return medicalRecordRepository.getMedicalRecordByUser(user)
    }

    override fun updateMedicalRecord(medicalRecord: MedicalRecord) {
        medicalRecordRepository.save(medicalRecord)
    }
}