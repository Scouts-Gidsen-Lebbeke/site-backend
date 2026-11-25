package be.sgl.backend.people.web

import be.sgl.backend.people.domain.BloodGroup
import be.sgl.backend.people.domain.MedicalRecord
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "MedicalRecord")
data class MedicalRecordDTO(
    val id: Int,
    val mayBePhotographed: Boolean,
    val mayTakePainkillers: Boolean,
    val foodAnomalies: String?,
    val allergies: String?,
    val activityRestrictions: String?,
    val familyRemarks: String?,
    val socialRemarks: String?,
    val diseases: String?,
    val medications: String?,
    val physician: String?,
    val physicianContact: String?,
    val bloodGroup: BloodGroup
) {
    companion object {
        fun of(record: MedicalRecord): MedicalRecordDTO {
            return MedicalRecordDTO(record.id!!, record.mayBePhotographed, record.mayTakePainkillers,
                record.foodAnomalies, record.allergies, record.activityRestrictions, record.familyRemarks,
                record.socialRemarks, record.diseases, record.medications, record.physician, record.physicianContact,
                record.bloodGroup)
        }
    }
}
