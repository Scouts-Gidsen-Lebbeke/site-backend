package be.sgl.backend.dto

import java.time.LocalDateTime

// read-only, no validation
data class ActivityRegistrationDTO(
    val id: Int?,
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
)