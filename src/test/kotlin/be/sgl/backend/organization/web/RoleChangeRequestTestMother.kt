package be.sgl.backend.organization.web

object RoleChangeRequestTestMother {

    fun memberRoleChangeRequest() = MemberRoleChangeRequestBuilder()

    fun staffRoleChangeRequest() = StaffRoleChangeRequestBuilder()

    class MemberRoleChangeRequestBuilder {
        private var externalId: String? = "requestExternalId"
        private var backupExternalId: String? = null
        private var name: String? = "requestRoleName"

        fun externalId(externalId: String?) = apply { this.externalId = externalId }

        fun backupExternalId(backupExternalId: String?) = apply { this.backupExternalId = backupExternalId }

        fun name(name: String?) = apply { this.name = name }

        fun build() = MemberRoleChangeRequest(externalId, backupExternalId, name)
    }

    class StaffRoleChangeRequestBuilder {
        private var externalId: String? = null
        private var backupExternalId: String? = null
        private var name: String? = "requestRoleName"
        private var staffLevel = false

        fun externalId(externalId: String?) = apply { this.externalId = externalId }

        fun backupExternalId(backupExternalId: String?) = apply { this.backupExternalId = backupExternalId }

        fun name(name: String?) = apply { this.name = name }

        fun staffLevel() = apply { this.staffLevel = true }

        fun build() = StaffRoleChangeRequest(externalId, backupExternalId, name, staffLevel)
    }
}
