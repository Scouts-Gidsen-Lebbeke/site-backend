package be.sgl.backend.entity.user

import be.sgl.backend.entity.branch.Branch
import jakarta.persistence.*

/**
 * We distinguish three types of roles:
 *  - Member roles: Roles with a linked [branch]. When a user pays for a membership in a certain branch,
 *    it gets assigned the linked role. This is only used for synchronization of external roles,
 *    thus should always have an external id. They also are never related to user rights, so their role level is always GUEST.
 *  - Staff roles: Roles with a linked [staffBranch]. When a user is assigned as staff of a certain branch,
 *    it gets assigned the linked role. This is used to list the staff of a branch and optionally for synchronization of external roles.
 *    A staff role can optionally give staff level user rights.
 *  - Admin roles: Roles with neither a [branch] nor a [staffBranch]. A user is an admin if it has a role of level ADMIN.
 *    This can furthermore also be used for external role synchronization.
 *
 * A role with both a branch and a staffBranch cannot exist.
 */
@Entity
class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    var externalId: String? = null
    var backupExternalId: String? = null
    lateinit var name: String
    @ManyToOne(fetch = FetchType.LAZY)
    var branch: Branch? = null
    @ManyToOne
    var staffBranch: Branch? = null
    @Column(nullable = false)
    var level = RoleLevel.GUEST

    val forExternalSync: Boolean
        get() = branch != null
    val memberRole: Boolean
        get() = branch != null && staffBranch == null && level == RoleLevel.GUEST
    val staffRole: Boolean
        get() = staffBranch != null && branch == null
    val adminRole: Boolean
        get() = level == RoleLevel.ADMIN

    companion object {

        fun memberRole(name: String, externalId: String, backupExternalId: String?, branch: Branch) = Role().apply {
            this.name = name
            this.externalId = externalId
            this.backupExternalId = backupExternalId
            this.branch = branch
            this.level = RoleLevel.GUEST
        }

        fun staffRole(name: String, externalId: String?, backupExternalId: String?, branch: Branch, staffLevel: Boolean) = Role().apply {
            this.name = name
            this.externalId = externalId
            this.backupExternalId = backupExternalId
            this.staffBranch = branch
            this.level = if (staffLevel) RoleLevel.STAFF else RoleLevel.GUEST
        }

        fun adminRole(name: String, externalId: String?, backupExternalId: String?) = Role().apply {
            this.name = name
            this.externalId = externalId
            this.backupExternalId = backupExternalId
            this.level = RoleLevel.ADMIN
        }
    }
}