package be.sgl.backend.controller.role

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.util.IntegrationTest
import be.sgl.backend.shared.util.PersistedUsers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
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

    private val users by lazy { PersistedUsers(userRepository, roleRepository, userRoleRepository) }

    @AfterEach
    fun cleanUp() {
        users.deleteAll()
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

    companion object {
        private const val ADMIN = "admin"
        private const val TARGET = "target"
    }
}
