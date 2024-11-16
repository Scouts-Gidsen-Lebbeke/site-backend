package be.sgl.backend.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn

@Entity
class MedicalRecord : Auditable() {
    @Id
    val id: Int? = null
    @OneToOne
    @PrimaryKeyJoinColumn
    lateinit var user: User
    var mayBePhotographed = false
    var mayTakePainkillers = false
    var foodAnomalies: String? = null
    var allergies: String? = null
    var activityRestrictions: String? = null
    var familyRemarks: String? = null
    var diseases: String? = null
    var medications: String? = null
    var physician: String? = null
}