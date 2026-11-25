package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRole
import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.shared.exception.UserRoleNotFoundException
import be.sgl.backend.shared.util.Usecase

@Usecase
class RevokeRole(
    private val userRoleRepository: UserRoleRepository,
    private val roleApi: RoleApi
) {

    fun execute(userId: Int, roleId: Int) {
        val assignment = userRoleRepository.findById(UserRole.Key(userId, roleId)).orElseThrow { UserRoleNotFoundException() }
        val role = roleApi.findRoles(listOf(roleId)).first()
        check(!role.memberRole) { "Member roles can't be manually deassigned!" }
        check(!role.adminRole || userRoleRepository.findByRoleId(roleId).any { it.userId != userId }) {
            "At least one admin should exist!"
        }
        userRoleRepository.delete(assignment)
    }
}
