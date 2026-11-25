package be.sgl.backend.organization.web

import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.repository.role.UserRoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.util.IntegrationTest
import be.sgl.backend.shared.util.PersistedUsers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@IntegrationTest
class RoleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository
    @Autowired
    private lateinit var userRepository: UserRepository

    private val users by lazy { PersistedUsers(userRepository, roleRepository) }

    @AfterEach
    fun cleanUp() {
        users.deleteAll()
        branchRepository.deleteAll()
    }

    private fun persistedStaffRole(): Role {
        val branch = branchRepository.save(branch().id(null).build())
        return roleRepository.save(staffRole(branch).id(null).build())
    }

    @Test
    fun `should delete role and its user roles when an admin deletes an assigned role`() {
        // given
        users.admin(ADMIN)
        val role = persistedStaffRole()
        val staff = userRepository.save(user().id(null).username(STAFF).build())
        userRoleRepository.save(userRole().user(staff).role(role).build())

        // when
        val result = mockMvc.perform(delete("/roles/${role.id}").with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isOk)
        assertThat(userRoleRepository.findAll().map { it.role.id }).doesNotContain(role.id)
        assertThat(roleRepository.existsById(role.id!!)).isFalse()
    }

    @Test
    fun `should return 403 when a non-admin deletes a role`() {
        // given
        users.withoutRoles(MEMBER)
        val role = persistedStaffRole()

        // when
        val result = mockMvc.perform(delete("/roles/${role.id}").with(jwt().jwt { it.subject(MEMBER) }))

        // then
        result.andExpect(status().isForbidden)
        assertThat(roleRepository.existsById(role.id!!)).isTrue()
    }

    companion object {
        private const val ADMIN = "admin"
        private const val MEMBER = "member"
        private const val STAFF = "staff"
    }
}
