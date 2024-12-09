package be.sgl.backend.dto

import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import java.time.LocalDateTime

data class ActivityRegistrationDTO(
    var activityName: String,
    var status: PaymentStatus,
    var price: Double,
    var completed: Boolean,
    var additionalData: String?,
    var start: LocalDateTime,
    var end: LocalDateTime
)