package be.sgl.backend.shared.util

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.repository.user.UserRepository

class PersistedUsers(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val userRoleRepository: UserRoleRepository
) {

    fun admin(username: String) {
        val role = roleRepository.save(adminRole().id(null).build())
        val admin = userRepository.save(user().id(null).username(username).build())
        userRoleRepository.save(userRole().userId(admin.id!!).roleId(role.id!!).build())
    }

    fun withoutRoles(username: String) {
        userRepository.save(user().id(null).username(username).build())
    }

    fun deleteAll() {
        userRoleRepository.deleteAll()
        userRepository.deleteAll()
        roleRepository.deleteAll()
    }
}
