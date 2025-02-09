package be.sgl.backend.entity.user

import be.sgl.backend.entity.Auditable
import jakarta.persistence.*
import java.time.LocalDate.now

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
    var socialRemarks: String? = null
    var diseases: String? = null
    var medications: String? = null
    var physician: String? = null
    var physicianContact: String? = null
    @Enumerated(EnumType.STRING)
    var bloodGroup = BloodGroup.UNKNOWN

    val isUpToDate: Boolean
        get() = lastModifiedDate?.let { now().minusYears(1).isBefore(it.toLocalDate()) } ?: false
    val needsConfirmation: Boolean
        get() = foodAnomalies != null || allergies != null || activityRestrictions != null || familyRemarks != null
                || socialRemarks != null || diseases != null || medications != null
    val needsCertificate: Boolean
        get() = medications != null
}