package be.sgl.backend.dto

import java.time.LocalDateTime

data class ActivityRegistrationDTO(
    val id: Int?,
    val activityName: String,
    val price: Double,
    val completed: Boolean,
    val additionalData: String?,
    val start: LocalDateTime,
    val end: LocalDateTime
)