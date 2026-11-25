package be.sgl.backend.setting.domain

import be.sgl.backend.setting.SettingId
import be.sgl.backend.setting.domain.SettingTestMother.setting
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SettingTest {

    @Test
    fun `should store the string representation when created with a non string value`() {
        // given
        val value = 12.5

        // when
        val testee = setting().id(SettingId.LATEST_DISPATCH_RATE).value(value).build()

        // then
        assertThat(testee.name).isEqualTo(SettingId.LATEST_DISPATCH_RATE.name)
        assertThat(testee.value).isEqualTo(value.toString())
    }
}
