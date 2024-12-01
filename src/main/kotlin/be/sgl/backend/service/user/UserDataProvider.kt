package be.sgl.backend.service.user

import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserRegistration

interface UserDataProvider {
    fun createRegistration(registration: UserRegistration): User
    fun acceptRegistration(registration: UserRegistration): User
    fun getUser(username: String) : User
    fun getUserWithAllData(username: String): User
    fun getMedicalRecord(user: User): MedicalRecord?
    fun updateMedicalRecord(medicalRecord: MedicalRecord)
}