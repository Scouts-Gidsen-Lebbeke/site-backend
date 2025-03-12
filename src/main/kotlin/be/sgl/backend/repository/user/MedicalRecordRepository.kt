package be.sgl.backend.repository.user

import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MedicalRecordRepository : JpaRepository<MedicalRecord, Int> {
    fun getMedicalRecordByUser(user: User): MedicalRecord?
}