package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User

data class ExternalMember(
    val userId: Int?,
    val firstName: String?,
    val name: String?,
    val email: String?,
    var syncState: SyncState
) {
    companion object {
        fun fromUser(user: User, syncState: SyncState): ExternalMember {
            return ExternalMember(user.id, user.firstName, user.name, user.email, syncState)
        }

        fun fromExternal(firstName: String?, lastName: String?, email: String?): ExternalMember {
            return ExternalMember(null, firstName, lastName, email, SyncState.OK)
        }
    }
}
