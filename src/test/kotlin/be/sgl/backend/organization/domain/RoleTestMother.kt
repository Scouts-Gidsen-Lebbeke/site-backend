package be.sgl.backend.organization.domain

import be.sgl.backend.shared.security.RoleLevel

object RoleTestMother {

    fun role() = RoleBuilder()

    /** A member role: linked to a [Branch], level GUEST. */
    fun memberRole(branch: Branch) = RoleBuilder().branch(branch).externalId("memberExternalId")

    /** A staff role: linked to a staff [Branch]. */
    fun staffRole(branch: Branch) = RoleBuilder().staffBranch(branch).level(RoleLevel.STAFF)

    /** An admin role: no branch, level ADMIN. */
    fun adminRole() = RoleBuilder().level(RoleLevel.ADMIN)

    class RoleBuilder {
        private var id: Int? = 1
        private var externalId: String? = null
        private var backupExternalId: String? = null
        private var name = "roleName"
        private var branch: Branch? = null
        private var staffBranch: Branch? = null
        private var level = RoleLevel.GUEST

        fun id(id: Int?) = apply { this.id = id }

        fun externalId(externalId: String?) = apply { this.externalId = externalId }

        fun backupExternalId(backupExternalId: String?) = apply { this.backupExternalId = backupExternalId }

        fun name(name: String) = apply { this.name = name }

        fun branch(branch: Branch?) = apply { this.branch = branch }

        fun staffBranch(staffBranch: Branch?) = apply { this.staffBranch = staffBranch }

        fun level(level: RoleLevel) = apply { this.level = level }

        fun build(): Role {
            val role = Role()
            role.id = id
            role.externalId = externalId
            role.backupExternalId = backupExternalId
            role.name = name
            role.branch = branch
            role.staffBranch = staffBranch
            role.level = level
            return role
        }
    }
}
