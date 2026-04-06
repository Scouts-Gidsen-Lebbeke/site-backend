package be.sgl.backend.dto.user

import be.sgl.backend.entity.user.User
import be.sgl.backend.service.user.sync.SyncState

// read-only, internal
data class ExternalMember(
    val userId: Int?,
    val firstName: String?,
    val name: String?,
    val email: String?,
    val externalId: String?,
    var syncState: SyncState?
) {
    companion object {
        fun fromUser(user: User, syncState: SyncState): ExternalMember {
            return ExternalMember(user.id, user.firstName, user.name, user.email, user.externalId, syncState)
        }

        fun fromExternal(firstName: String?, lastName: String?, email: String?, externalId: String?): ExternalMember {
            return ExternalMember(null, firstName, lastName, email, externalId, null)
        }
    }
}
