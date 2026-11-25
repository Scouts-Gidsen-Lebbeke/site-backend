package be.sgl.backend.setting.web

import be.sgl.backend.setting.domain.SettingTestMother.setting
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.setting.SettingApi
import be.sgl.backend.setting.SettingId
import be.sgl.backend.setting.domain.SettingRepository
import be.sgl.backend.shared.util.IntegrationTest
import be.sgl.backend.shared.util.PersistedUsers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@IntegrationTest
class SettingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var settingApi: SettingApi
    @Autowired
    private lateinit var settingRepository: SettingRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository

    private val settingId = SettingId.CALENDAR_NAME.name
    private val users by lazy { PersistedUsers(userRepository, roleRepository) }

    @AfterEach
    fun cleanUp() {
        settingRepository.deleteAll()
        users.deleteAll()
    }

    @Test
    fun `should return the value without authentication when the setting exists`() {
        // given
        val value = "Kalender"
        settingRepository.save(setting().name(settingId).value(value).build())

        // when
        val result = mockMvc.perform(get("/settings/$settingId"))

        // then
        result.andExpect(status().isOk).andExpect(content().string(value))
    }

    @Test
    fun `should forbid the update when the user is not an admin`() {
        // given
        users.withoutRoles(MEMBER)

        // when
        val result = mockMvc.perform(put("/settings/$settingId").param("value", "x").with(jwt().jwt { it.subject(MEMBER) }))

        // then
        result.andExpect(status().isForbidden)
        assertThat(settingApi.get(settingId)).isNull()
    }

    @Test
    fun `should store the value when an admin updates the setting`() {
        // given
        users.admin(ADMIN)
        val value = "Nieuwe kalender"

        // when
        val result = mockMvc.perform(put("/settings/$settingId").param("value", value).with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isOk)
        assertThat(settingApi.get(settingId)).isEqualTo(value)
    }

    @Test
    fun `should delete the setting when an admin updates it without a value`() {
        // given
        users.admin(ADMIN)
        settingRepository.save(setting().name(settingId).build())

        // when
        val result = mockMvc.perform(put("/settings/$settingId").with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isOk)
        assertThat(settingApi.get(settingId)).isNull()
    }

    companion object {
        private const val ADMIN = "admin"
        private const val MEMBER = "member"
    }
}
