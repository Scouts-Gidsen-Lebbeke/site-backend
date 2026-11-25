package be.sgl.backend.controller.role

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.util.IntegrationTest
import be.sgl.backend.shared.util.PersistedUsers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@IntegrationTest
class UserRoleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository

    private val users by lazy { PersistedUsers(userRepository, roleRepository, userRoleRepository) }

    @AfterEach
    fun cleanUp() {
        users.deleteAll()
        branchRepository.deleteAll()
    }

    @Test
    fun `should return the new admin level when marking a user as admin`() {
        // given
        users.admin(ADMIN)
        users.withoutRoles(TARGET)

        // when
        val result = mockMvc.perform(post("/user-roles/admin")
            .with(jwt().jwt { it.subject(ADMIN) })
            .contentType(MediaType.APPLICATION_JSON)
            .content(TARGET))

        // then
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.user.username").value(TARGET))
            .andExpect(jsonPath("$.user.level").value(RoleLevel.ADMIN.name))
    }

    @Test
    fun `should remove staff role when admin deassigns it`() {
        // given
        users.admin(ADMIN)
        val targetId = userRepository.save(user().id(null).username(TARGET).build()).id!!
        val branch = branchRepository.save(branch().id(null).build())
        val staffRoleId = roleRepository.save(staffRole(branch).id(null).build()).id!!
        userRoleRepository.save(userRole().userId(targetId).roleId(staffRoleId).build())

        // when
        val result = mockMvc.perform(delete("/user-roles/role/{roleId}/user/{username}", staffRoleId, TARGET)
            .with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isOk)
        assertThat(userRoleRepository.findByRoleId(staffRoleId)).isEmpty()
    }

    @Test
    fun `should return 400 when deassigning the last admin`() {
        // given
        val adminRoleId = users.admin(ADMIN)

        // when
        val result = mockMvc.perform(delete("/user-roles/role/{roleId}/user/{username}", adminRoleId, ADMIN)
            .with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 403 when a non-admin deassigns`() {
        // given
        val adminRoleId = users.admin(ADMIN)
        users.withoutRoles(TARGET)

        // when
        val result = mockMvc.perform(delete("/user-roles/role/{roleId}/user/{username}", adminRoleId, ADMIN)
            .with(jwt().jwt { it.subject(TARGET) }))

        // then
        result.andExpect(status().isForbidden)
    }

    @Test
    fun `should return 404 when the user does not hold the role`() {
        // given
        val adminRoleId = users.admin(ADMIN)
        users.withoutRoles(TARGET)

        // when
        val result = mockMvc.perform(delete("/user-roles/role/{roleId}/user/{username}", adminRoleId, TARGET)
            .with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isNotFound)
    }

    companion object {
        private const val ADMIN = "admin"
        private const val TARGET = "target"
    }
}
