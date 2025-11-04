package be.sgl.backend.service

import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.repository.SettingRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class SettingServiceTest {

    @Mock
    private lateinit var settingRepository: SettingRepository

    @InjectMocks
    private lateinit var settingService: SettingService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `get should return value when setting exists`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val setting = Setting(settingId, "Test Organization")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(setting)

        val result = settingService.get(settingId)

        assertEquals("Test Organization", result)
        verify(settingRepository).findByIdOrNull(settingId.name)
    }

    @Test
    fun `get should return null when setting does not exist`() {
        val settingId = SettingId.ORGANIZATION_NAME
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(null)

        val result = settingService.get(settingId)

        assertNull(result)
        verify(settingRepository).findByIdOrNull(settingId.name)
    }

    @Test
    fun `getOrDefault with String default should return value when setting exists`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val setting = Setting(settingId, "Test Organization")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(setting)

        val result = settingService.getOrDefault(settingId, "Default Name")

        assertEquals("Test Organization", result)
    }

    @Test
    fun `getOrDefault with String default should return default when setting does not exist`() {
        val settingId = SettingId.ORGANIZATION_NAME
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(null)

        val result = settingService.getOrDefault(settingId, "Default Name")

        assertEquals("Default Name", result)
    }

    @Test
    fun `getOrDefault with Double default should return value when setting exists`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val setting = Setting(settingId, "42.5")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(setting)

        val result = settingService.getOrDefault(settingId, 0.0)

        assertEquals(42.5, result)
    }

    @Test
    fun `getOrDefault with Double default should return default when setting does not exist`() {
        val settingId = SettingId.ORGANIZATION_NAME
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(null)

        val result = settingService.getOrDefault(settingId, 10.0)

        assertEquals(10.0, result)
    }

    @Test
    fun `update should create new setting when it does not exist`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val newSetting = Setting(settingId, "New Organization")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(null)
        `when`(settingRepository.save(any(Setting::class.java))).thenReturn(newSetting)

        settingService.update(settingId, "New Organization")

        verify(settingRepository).findByIdOrNull(settingId.name)
        verify(settingRepository).save(any(Setting::class.java))
    }

    @Test
    fun `update should update existing setting when it exists`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val existingSetting = Setting(settingId, "Old Organization")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(existingSetting)
        `when`(settingRepository.save(existingSetting)).thenReturn(existingSetting)

        settingService.update(settingId, "Updated Organization")

        assertEquals("Updated Organization", existingSetting.value)
        verify(settingRepository).save(existingSetting)
    }

    @Test
    fun `update should delete setting when value is null`() {
        val settingId = SettingId.ORGANIZATION_NAME

        settingService.update(settingId, null)

        verify(settingRepository).deleteById(settingId.name)
        verify(settingRepository, never()).save(any(Setting::class.java))
    }

    @Test
    fun `update should handle numeric values`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val existingSetting = Setting(settingId, "0")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(existingSetting)
        `when`(settingRepository.save(existingSetting)).thenReturn(existingSetting)

        settingService.update(settingId, 42)

        assertEquals("42", existingSetting.value)
        verify(settingRepository).save(existingSetting)
    }

    @Test
    fun `update should handle boolean values`() {
        val settingId = SettingId.ORGANIZATION_NAME
        val existingSetting = Setting(settingId, "false")
        `when`(settingRepository.findByIdOrNull(settingId.name)).thenReturn(existingSetting)
        `when`(settingRepository.save(existingSetting)).thenReturn(existingSetting)

        settingService.update(settingId, true)

        assertEquals("true", existingSetting.value)
        verify(settingRepository).save(existingSetting)
    }
}
