package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.activity.Activity
import java.time.LocalDateTime

// DTO for statistics list overview
data class ActivityResult(
    val id: Int,
    val name: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val closed: LocalDateTime,
    val cancellable: Boolean,
    val registrationCount: Int,
    val totalPrice: Double,
    val status: RegistrableStatus
) {
    companion object {
        fun of(activity: Activity, registrations: List<Double>): ActivityResult {
            return ActivityResult(activity.id!!, activity.name, activity.start, activity.end, activity.closed,
                activity.cancellable, registrations.count(), registrations.sum(), activity.getStatus())
        }
    }
}