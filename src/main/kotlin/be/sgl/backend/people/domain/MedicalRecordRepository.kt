package be.sgl.backend.people.domain

import be.sgl.backend.people.domain.MedicalRecord
import be.sgl.backend.people.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MedicalRecordRepository : JpaRepository<MedicalRecord, Int> {
    fun findMedicalRecordByUser(user: User): MedicalRecord?
}