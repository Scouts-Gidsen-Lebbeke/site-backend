package be.sgl.backend.service.user

import be.sgl.backend.entity.MedicalRecord
import be.sgl.backend.entity.User
import be.sgl.backend.entity.UserRegistration
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class InternalUserDataProvider : UserDataProvider {
    override fun createRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun acceptRegistration(registration: UserRegistration): User {
        TODO("Not yet implemented")
    }

    override fun getUser(username: String): User {
        TODO("Not yet implemented")
    }

    override fun getUserWithAllData(username: String): User {
        TODO("Not yet implemented")
    }

    override fun getMedicalRecord(username: String): MedicalRecord {
        TODO("Not yet implemented")
    }

    override fun updateMedicalRecord(username: String) {
        TODO("Not yet implemented")
    }
}