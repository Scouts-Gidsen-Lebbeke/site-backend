package be.sgl.backend.dto

import java.time.LocalDateTime

data class ActivityRegistrationDTO(
    val id: Int?,
    val price: Double,
    val completed: Boolean,
    val additionalData: String?,
    val user: UserDTO,
    val start: LocalDateTime,
    val end: LocalDateTime
)