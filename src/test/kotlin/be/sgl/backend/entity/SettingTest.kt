package be.sgl.backend.entity

import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SettingTest {

    @Test
    fun `Setting constructor with SettingId should set name correctly`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "My Calendar")

        assertEquals("CALENDAR_NAME", setting.name)
        assertEquals("My Calendar", setting.value)
    }

    @Test
    fun `Setting constructor should convert value to string`() {
        val setting = Setting(SettingId.LATEST_DISPATCH_RATE, 42)

        assertEquals("42", setting.value)
    }

    @Test
    fun `Setting constructor should handle double values`() {
        val setting = Setting(SettingId.LATEST_DISPATCH_RATE, 42.5)

        assertEquals("42.5", setting.value)
    }

    @Test
    fun `Setting constructor should handle boolean values`() {
        val setting = Setting(SettingId.CALENDAR_NAME, true)

        assertEquals("true", setting.value)
    }

    @Test
    fun `Setting can be created with no-arg constructor`() {
        val setting = Setting()

        assertNotNull(setting)
    }

    @Test
    fun `Setting name can be set after construction`() {
        val setting = Setting()
        setting.name = "TEST_SETTING"
        setting.value = "test value"

        assertEquals("TEST_SETTING", setting.name)
        assertEquals("test value", setting.value)
    }

    @Test
    fun `Setting value can be updated`() {
        val setting = Setting(SettingId.CALENDAR_NAME, "Old Value")
        setting.value = "New Value"

        assertEquals("New Value", setting.value)
    }
}
