package be.sgl.backend.controller

import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.service.SettingService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.springframework.http.HttpStatus

class SettingControllerTest {

    @Mock
    private lateinit var settingService: SettingService

    @InjectMocks
    private lateinit var settingController: SettingController

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `getSetting should return setting value when it exists`() {
        val settingId = SettingId.CALENDAR_NAME
        `when`(settingService.get(settingId)).thenReturn("Test Calendar")

        val response = settingController.getSetting(settingId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Test Calendar", response.body)
        verify(settingService).get(settingId)
    }

    @Test
    fun `getSetting should return null when setting does not exist`() {
        val settingId = SettingId.CALENDAR_NAME
        `when`(settingService.get(settingId)).thenReturn(null)

        val response = settingController.getSetting(settingId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNull(response.body)
        verify(settingService).get(settingId)
    }

    @Test
    fun `updateSetting should update setting value`() {
        val settingId = SettingId.CALENDAR_NAME
        val value = "New Calendar Name"

        val response = settingController.updateSetting(settingId, value)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(settingService).update(settingId, value)
    }

    @Test
    fun `updateSetting should handle null value`() {
        val settingId = SettingId.CALENDAR_NAME

        val response = settingController.updateSetting(settingId, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(settingService).update(settingId, null)
    }

    @Test
    fun `updateSetting should handle empty value`() {
        val settingId = SettingId.CALENDAR_NAME
        val value = ""

        val response = settingController.updateSetting(settingId, value)

        assertEquals(HttpStatus.OK, response.statusCode)
        verify(settingService).update(settingId, value)
    }

    @Test
    fun `updateSetting should handle multiple different setting IDs`() {
        val settings = listOf(
            SettingId.CALENDAR_NAME to "Calendar",
            SettingId.ORGANIZATION_NAME to "Organization",
            SettingId.REPRESENTATIVE_TITLE to "Title",
            SettingId.REPRESENTATIVE_USERNAME to "Username"
        )

        settings.forEach { (id, value) ->
            val response = settingController.updateSetting(id, value)
            assertEquals(HttpStatus.OK, response.statusCode)
        }

        settings.forEach { (id, value) ->
            verify(settingService).update(id, value)
        }
    }
}
