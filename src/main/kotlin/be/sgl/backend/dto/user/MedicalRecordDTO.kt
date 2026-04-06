package be.sgl.backend.dto.user

import be.sgl.backend.entity.user.BloodGroup
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
)