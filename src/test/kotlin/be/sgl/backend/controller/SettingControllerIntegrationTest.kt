package be.sgl.backend.controller

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.repository.SettingRepository
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class SettingControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var settingRepository: SettingRepository

    @BeforeEach
    fun setup() {
        settingRepository.deleteAll()
    }

    @Test
    fun `GET setting should return setting value when it exists`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Test Calendar")
        settingRepository.save(setting)
        settingRepository.flush()

        mockMvc.perform(get("/settings/${SettingId.CALENDAR_NAME.name}"))
            .andExpect(status().isOk)
            .andExpect(content().string("Test Calendar"))
    }

    @Test
    fun `GET setting should return empty when setting does not exist`() {
        mockMvc.perform(get("/settings/${SettingId.CALENDAR_NAME.name}"))
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT setting should create new setting`() {
        mockMvc.perform(
            put("/settings/${SettingId.CALENDAR_NAME.name}")
                .param("value", "New Calendar")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(status().isOk)

        val saved = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assert(saved != null)
        assert(saved?.value == "New Calendar")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT setting should update existing setting`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Old Value")
        settingRepository.save(setting)
        settingRepository.flush()

        mockMvc.perform(
            put("/settings/${SettingId.CALENDAR_NAME.name}")
                .param("value", "Updated Value")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(status().isOk)

        val updated = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assert(updated?.value == "Updated Value")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT setting with null value should delete setting`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "To Delete")
        settingRepository.save(setting)
        settingRepository.flush()

        mockMvc.perform(
            put("/settings/${SettingId.CALENDAR_NAME.name}")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(status().isOk)

        val deleted = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assert(deleted == null)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `PUT setting should require ADMIN role`() {
        mockMvc.perform(
            put("/settings/${SettingId.CALENDAR_NAME.name}")
                .param("value", "New Value")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT setting should handle numeric values`() {
        mockMvc.perform(
            put("/settings/${SettingId.LATEST_DISPATCH_RATE.name}")
                .param("value", "42.5")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(status().isOk)

        val saved = settingRepository.findByIdOrNull(SettingId.LATEST_DISPATCH_RATE.name)
        assert(saved?.value == "42.5")
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `PUT setting should handle empty string`() {
        mockMvc.perform(
            put("/settings/${SettingId.CALENDAR_NAME.name}")
                .param("value", "")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        )
            .andExpect(status().isOk)

        val saved = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assert(saved?.value == "")
    }

    @Test
    fun `GET setting should be publicly accessible`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Public Calendar")
        settingRepository.save(setting)
        settingRepository.flush()

        // No authentication
        mockMvc.perform(get("/settings/${SettingId.CALENDAR_NAME.name}"))
            .andExpect(status().isOk)
            .andExpect(content().string("Public Calendar"))
    }
}
