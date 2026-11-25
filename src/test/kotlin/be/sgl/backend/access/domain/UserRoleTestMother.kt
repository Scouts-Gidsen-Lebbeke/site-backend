package be.sgl.backend.access.domain

object UserRoleTestMother {

    fun userRole() = UserRoleBuilder()

    class UserRoleBuilder {
        private var userId = 1
        private var roleId = 1

        fun userId(userId: Int) = apply { this.userId = userId }

        fun roleId(roleId: Int) = apply { this.roleId = roleId }

        fun build() = UserRole(userId, roleId)
    }
}
