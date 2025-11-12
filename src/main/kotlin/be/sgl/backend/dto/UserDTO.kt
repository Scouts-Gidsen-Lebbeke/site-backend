package be.sgl.backend.dto

import be.sgl.backend.entity.user.BloodGroup
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.util.Nis
import be.sgl.backend.util.PhoneNumber
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class UserDTO(
    val username: String?,
    @field:NotBlank
    val name: String?,
    @field:NotBlank
    val firstName: String?,
    @field:Email
    @field:NotBlank
    val email: String?,
    val image: String?,
    val level: RoleLevel,
    val memberId: String?,
    @field:NotNull
    val birthdate: LocalDate?,
    @field:PhoneNumber
    val mobile: String?,
    @field:Nis
    val nis: String?,
    val accountNo: String?,
    @field:NotNull
    val sex: Sex?,
    val hasReduction: Boolean = false,
    val hasHandicap: Boolean = false
)

// read-only
data class StaffDTO(
    val name: String,
    val firstName: String,
    val image: String?,
    val nickname: String?,
    val totem: String?,
)

data class MedicalRecordDTO(
    var id: Int? = null,
    var mayBePhotographed: Boolean,
    var mayTakePainkillers: Boolean,
    var foodAnomalies: String?,
    var allergies: String?,
    var activityRestrictions: String?,
    var familyRemarks: String?,
    var socialRemarks: String?,
    var diseases: String?,
    var medications: String?,
    var physician: String?,
    var physicianContact: String?,
    var bloodGroup: BloodGroup
)