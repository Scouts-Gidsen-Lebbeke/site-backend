package be.sgl.backend.service

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.repository.SettingRepository
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class SettingServiceIntegrationTest {

    @Autowired
    private lateinit var settingService: SettingService

    @Autowired
    private lateinit var settingRepository: SettingRepository

    @BeforeEach
    fun setup() {
        settingRepository.deleteAll()
    }

    @Test
    fun `get should return value from database`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Integration Test Calendar")
        settingRepository.save(setting)
        settingRepository.flush()

        val result = settingService.get(SettingId.CALENDAR_NAME)

        assertEquals("Integration Test Calendar", result)
    }

    @Test
    fun `get should return null when setting does not exist in database`() {
        val result = settingService.get(SettingId.CALENDAR_NAME)

        assertNull(result)
    }

    @Test
    fun `getOrDefault with String should return value from database`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Database Calendar")
        settingRepository.save(setting)
        settingRepository.flush()

        val result = settingService.getOrDefault(SettingId.CALENDAR_NAME, "Default Calendar")

        assertEquals("Database Calendar", result)
    }

    @Test
    fun `getOrDefault with String should return default when not in database`() {
        val result = settingService.getOrDefault(SettingId.CALENDAR_NAME, "Default Calendar")

        assertEquals("Default Calendar", result)
    }

    @Test
    fun `getOrDefault with Double should parse value from database`() {
        val setting = Setting(SettingId.LATEST_DISPATCH_RATE, "42.75")
        settingRepository.save(setting)
        settingRepository.flush()

        val result = settingService.getOrDefault(SettingId.LATEST_DISPATCH_RATE, 0.0)

        assertEquals(42.75, result)
    }

    @Test
    fun `getOrDefault with Double should return default when not in database`() {
        val result = settingService.getOrDefault(SettingId.LATEST_DISPATCH_RATE, 10.0)

        assertEquals(10.0, result)
    }

    @Test
    fun `update should persist new setting to database`() {
        settingService.update(SettingId.CALENDAR_NAME, "Persisted Calendar")

        val saved = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assertNotNull(saved)
        assertEquals("Persisted Calendar", saved?.value)
    }

    @Test
    fun `update should modify existing setting in database`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Original")
        settingRepository.save(setting)
        settingRepository.flush()

        settingService.update(SettingId.CALENDAR_NAME, "Modified")

        val updated = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assertEquals("Modified", updated?.value)
    }

    @Test
    fun `update with null should delete setting from database`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "To Delete")
        settingRepository.save(setting)
        settingRepository.flush()

        settingService.update(SettingId.CALENDAR_NAME, null)

        val deleted = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assertNull(deleted)
    }

    @Test
    fun `update should handle numeric values and persist correctly`() {
        settingService.update(SettingId.LATEST_DISPATCH_RATE, 99.99)

        val saved = settingRepository.findByIdOrNull(SettingId.LATEST_DISPATCH_RATE.name)
        assertEquals("99.99", saved?.value)

        // Should be able to retrieve as double
        val retrieved = settingService.getOrDefault(SettingId.LATEST_DISPATCH_RATE, 0.0)
        assertEquals(99.99, retrieved)
    }

    @Test
    fun `update should handle boolean values and persist correctly`() {
        settingService.update(SettingId.CALENDAR_NAME, true)

        val saved = settingRepository.findByIdOrNull(SettingId.CALENDAR_NAME.name)
        assertEquals("true", saved?.value)
    }

    @Test
    fun `multiple updates should persist correctly`() {
        settingService.update(SettingId.CALENDAR_NAME, "First")
        settingService.update(SettingId.ORGANIZATION_NAME, "Org Name")
        settingService.update(SettingId.REPRESENTATIVE_TITLE, "Title")

        assertEquals("First", settingService.get(SettingId.CALENDAR_NAME))
        assertEquals("Org Name", settingService.get(SettingId.ORGANIZATION_NAME))
        assertEquals("Title", settingService.get(SettingId.REPRESENTATIVE_TITLE))
    }

    @Test
    fun `settings should persist across service calls`() {
        settingService.update(SettingId.CALENDAR_NAME, "Persistent Value")
        settingRepository.flush()

        // Simulate new service usage
        val retrieved1 = settingService.get(SettingId.CALENDAR_NAME)
        val retrieved2 = settingService.get(SettingId.CALENDAR_NAME)

        assertEquals("Persistent Value", retrieved1)
        assertEquals("Persistent Value", retrieved2)
    }
}
