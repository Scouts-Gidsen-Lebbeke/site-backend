package be.sgl.backend.entity.registrable

import java.time.LocalDateTime

enum class RegistrableStatus {
    NOT_YET_OPEN,
    REGISTRATIONS_OPENED,
    REGISTRATIONS_COMPLETED,
    STARTED,
    COMPLETED,
    CANCELLED;

    companion object {
        fun Registrable.getStatus() = when {
            cancelled -> CANCELLED
            LocalDateTime.now() < open -> NOT_YET_OPEN
            LocalDateTime.now() < closed -> REGISTRATIONS_OPENED
            LocalDateTime.now() < start -> REGISTRATIONS_COMPLETED
            LocalDateTime.now() < end -> STARTED
            else -> COMPLETED
        }
    }
}