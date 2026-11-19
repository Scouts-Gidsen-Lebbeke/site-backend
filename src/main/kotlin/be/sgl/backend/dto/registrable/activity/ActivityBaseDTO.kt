package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.entity.registrable.activity.Activity
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "ActivityBase", description = "Basic information about an activity.")
data class ActivityBaseDTO(
    val id: Int,
    val name: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val closed: LocalDateTime,
    val cancellable: Boolean
) {
    companion object {
        fun of(activity: Activity): ActivityBaseDTO {
            return ActivityBaseDTO(activity.id!!, activity.name, activity.start, activity.end, activity.closed,
                activity.cancellable)
        }
    }
}
