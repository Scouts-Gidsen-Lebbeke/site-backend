package be.sgl.backend.service.user

import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.User

interface UserDataProvider {
    fun registerUser(registrationDTO: UserRegistrationDTO): User
    fun acceptRegistration(user: User)
    fun denyRegistration(user: User)

    fun getUser(username: String) : User
    fun getUserWithAllData(username: String): User
    fun updateUser(user: User): User
    fun deleteUser(username: String)
    fun addRole(user: User, role: Role)
    fun deleteRole(user: User, role: Role)

    fun getMedicalRecord(user: User): MedicalRecord?
    fun updateMedicalRecord(medicalRecord: MedicalRecord)
}