package be.sgl.backend.repository

import be.sgl.backend.entity.MedicalRecord
import be.sgl.backend.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MedicalRecordRepository : JpaRepository<MedicalRecord, Int> {
    fun getMedicalRecordByUser(user: User): MedicalRecord?
}