package be.sgl.backend.shared.util

import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository

class PersistedUsers(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    fun admin(username: String) {
        val role = roleRepository.save(adminRole().id(null).build())
        val admin = user().id(null).username(username).build()
        userRole().user(admin).role(role).build()
        userRepository.save(admin)
    }

    fun withoutRoles(username: String) {
        userRepository.save(user().id(null).username(username).build())
    }

    fun deleteAll() {
        userRepository.deleteAll()
        roleRepository.deleteAll()
    }
}
