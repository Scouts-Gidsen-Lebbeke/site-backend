package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "ActivityRegistration")
data class ActivityRegistrationDTO(
    val id: Int,
    val price: Double,
    val paid: Boolean,
    val completed: Boolean,
    val additionalData: String?,
    val user: UserDTO,
    val restriction: ActivityRestrictionDTO,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val createdDate: LocalDateTime,
    val subscribable: ActivityBaseDTO
) {
    companion object {
        fun of(registration: ActivityRegistration): ActivityRegistrationDTO {
            return ActivityRegistrationDTO(registration.id!!, registration.price, registration.paid,
                registration.completed, registration.additionalData, UserDTO.of(registration.user),
                ActivityRestrictionDTO.of(registration.restriction), registration.start, registration.end,
                registration.createdDate!!, ActivityBaseDTO.of(registration.subscribable))
        }
    }
}
