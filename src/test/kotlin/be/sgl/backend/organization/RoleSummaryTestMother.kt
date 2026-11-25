package be.sgl.backend.organization

import be.sgl.backend.shared.security.RoleLevel

object RoleSummaryTestMother {

    fun roleSummary() = RoleSummaryBuilder()

    fun memberRoleSummary(branchId: Int) = RoleSummaryBuilder().branchId(branchId).externalId("memberExternalId")

    fun staffRoleSummary(staffBranchId: Int) = RoleSummaryBuilder().staffBranchId(staffBranchId).level(RoleLevel.STAFF)

    fun adminRoleSummary() = RoleSummaryBuilder().level(RoleLevel.ADMIN)

    class RoleSummaryBuilder {
        private var id = 1
        private var name = "roleName"
        private var level = RoleLevel.GUEST
        private var branchId: Int? = null
        private var staffBranchId: Int? = null
        private var externalId: String? = null
        private var backupExternalId: String? = null

        fun id(id: Int) = apply { this.id = id }

        fun name(name: String) = apply { this.name = name }

        fun level(level: RoleLevel) = apply { this.level = level }

        fun branchId(branchId: Int?) = apply { this.branchId = branchId }

        fun staffBranchId(staffBranchId: Int?) = apply { this.staffBranchId = staffBranchId }

        fun externalId(externalId: String?) = apply { this.externalId = externalId }

        fun backupExternalId(backupExternalId: String?) = apply { this.backupExternalId = backupExternalId }

        fun build() = RoleSummary(id, name, level, branchId, staffBranchId, externalId, backupExternalId)
    }
}
