package be.sgl.backend.entity.role

import be.sgl.backend.entity.user.User
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleTestMother
import be.sgl.backend.entity.user.UserTestMother

object UserRoleTestMother {

    fun userRole() = UserRoleBuilder()

    class UserRoleBuilder {
        private var user = UserTestMother.user().build()
        private var role = RoleTestMother.role().build()

        fun user(user: User) = apply { this.user = user }

        fun role(role: Role) = apply { this.role = role }

        /** Builds the user role and also adds it to the user's roles, as the application does. */
        fun build(): UserRole {
            val userRole = UserRole(user, role)
            user.roles.add(userRole)
            return userRole
        }
    }
}
