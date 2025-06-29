package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.User

data class ExternalMember(
    val userId: Int?,
    val firstName: String?,
    val name: String?,
    val email: String?
) {
    companion object {
        fun fromUser(user: User): ExternalMember {
            return ExternalMember(user.id, user.firstName, user.name, user.email)
        }

        fun fromExternal(firstName: String?, lastName: String?, email: String?): ExternalMember {
            return ExternalMember(null, firstName, lastName, email)
        }
    }
}
