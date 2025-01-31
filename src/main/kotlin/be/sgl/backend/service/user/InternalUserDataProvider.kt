package be.sgl.backend.service.user

import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserRegistration
import be.sgl.backend.repository.MedicalRecordRepository
import be.sgl.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service

@Service
@Conditional(InternalOrganizationCondition::class)
class InternalUserDataProvider : UserDataProvider {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    override fun registerUser(registrationDTO: UserRegistrationDTO): User {
        val user = User(registrationDTO)
        return userRepository.save(user)
    }

    override fun acceptRegistration(user: User) {
        // generate a username for the user
        // send email with invite for profile completion
    }

    override fun denyRegistration(user: User) {
        userRepository.delete(user)
    }

    override fun getUser(username: String): User {
        return userRepository.getUserByUsernameEquals(username)
    }

    override fun getUserWithAllData(username: String): User {
        return userRepository.getUserByUsernameEqualsAndUserData(username)
    }

    override fun updateUser(user: User): User {
        TODO("Not yet implemented")
    }

    override fun deleteUser(username: String) {
        TODO("Not yet implemented")
    }

    override fun addRole(user: User, role: Role) {
        TODO("Not yet implemented")
    }

    override fun deleteRole(user: User, role: Role) {
        TODO("Not yet implemented")
    }

    override fun getMedicalRecord(user: User): MedicalRecord? {
        return medicalRecordRepository.getMedicalRecordByUser(user)
    }

    override fun updateMedicalRecord(medicalRecord: MedicalRecord) {
        medicalRecordRepository.save(medicalRecord)
    }
}