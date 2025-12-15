package be.sgl.backend.dto.registrable.activity

import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import java.time.LocalDateTime

/**
 * DTO for user feedback about the current activity:
 *  - First of all, the activity should be open for registrations. This check should be happened on activity retrieval.
 *  - The user can only register if he hasn't already a paid registration.
 *  - If the user has a pending registration, it should be finished before creating another one.
 *  - The user can only register when he has an active branch membership.
 *  - When he has an active branch, it should have a matching restriction, meaning not both of the options are empty.
 *  - When an option is present for this member, it should be part of the open options (otherwise the limit is reached).
 *  - When it has one or more valid restrictions, its medical info should be present.
 *  - When its medical info is existing, it should still be up to date (according to the up-to-date flag).
 */
// read-only, internal
data class ActivityRegistrationStatus(
    val currentRegistration: ActivityRegistration? = null,
    val activeMembership: Boolean = true,
    val openOptions: List<ActivityRestriction> = emptyList(),
    val closedOptions: List<ActivityRestriction> = emptyList(),
    val medicsDate: LocalDateTime? = null,
    val medicalsUpToDate: Boolean = false
)