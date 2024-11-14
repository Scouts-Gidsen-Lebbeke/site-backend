package be.sgl.backend.service.user

import be.sgl.backend.entity.MedicalRecord
import be.sgl.backend.entity.User
import be.sgl.backend.entity.UserRegistration

interface UserDataProvider {
    fun createRegistration(registration: UserRegistration): User
    fun acceptRegistration(registration: UserRegistration): User
    fun getUser(username: String) : User
    fun getUserWithAllData(username: String): User
    fun getMedicalRecord(username: String): MedicalRecord
    fun updateMedicalRecord(username: String)
}