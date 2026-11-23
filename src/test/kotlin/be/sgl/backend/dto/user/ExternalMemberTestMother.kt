package be.sgl.backend.dto.user

import be.sgl.backend.service.user.sync.SyncState

object ExternalMemberTestMother {

    fun externalMember() = ExternalMemberBuilder()

    class ExternalMemberBuilder {
        private var userId: Int? = null
        private var firstName: String? = "firstName"
        private var name: String? = "lastName"
        private var email: String? = "firstName.lastName@corp.com"
        private var externalId: String? = "externalId"
        private var syncState: SyncState? = null

        fun userId(userId: Int) = apply { this.userId = userId }

        fun firstName(firstName: String?) = apply { this.firstName = firstName }

        fun name(name: String?) = apply { this.name = name }

        fun email(email: String?) = apply { this.email = email }

        fun externalId(externalId: String?) = apply { this.externalId = externalId }

        fun syncState(syncState: SyncState) = apply { this.syncState = syncState }

        fun build() = ExternalMember(userId, firstName, name, email, externalId, syncState)
    }
}
