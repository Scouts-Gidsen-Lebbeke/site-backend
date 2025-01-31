package be.sgl.backend.dto

import java.time.LocalDateTime

data class ActivityRegistrationDTO(
    var activityName: String,
    var price: Double,
    var completed: Boolean,
    var additionalData: String?,
    var start: LocalDateTime,
    var end: LocalDateTime
)