package be.sgl.backend.service

import be.sgl.backend.entity.setting.Setting
import be.sgl.backend.entity.setting.SettingTestMother.setting
import be.sgl.backend.repository.SettingRepository
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SettingServiceTest {

    @Mock
    lateinit var settingRepository: SettingRepository
    @InjectMocks
    lateinit var testee: SettingService

    private val id = "settingId"

    private fun givenStored(value: String) {
        given(settingRepository.findById(id)).willReturn(Optional.of(setting().name(id).value(value).build()))
    }

    private fun givenNothingStored() {
        given(settingRepository.findById(id)).willReturn(Optional.empty())
    }

    @Nested
    inner class Get {

        @Test
        fun `should return the stored value when the setting exists`() {
            // given
            givenStored("value")

            // when
            val result = testee.get(id)

            // then
            assertThat(result).isEqualTo("value")
        }

        @Test
        fun `should return null when the setting does not exist`() {
            // given
            givenNothingStored()

            // when
            val result = testee.get(id)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should return the default string when the setting does not exist`() {
            // given
            givenNothingStored()

            // when
            val result = testee.getOrDefault(id, "default")

            // then
            assertThat(result).isEqualTo("default")
        }

        @Test
        fun `should return the stored string instead of the default when the setting exists`() {
            // given
            givenStored("value")

            // when
            val result = testee.getOrDefault(id, "default")

            // then
            assertThat(result).isEqualTo("value")
        }

        @Test
        fun `should parse the stored double when the setting exists`() {
            // given
            givenStored("0.42")

            // when
            val result = testee.getOrDefault(id, 1.0)

            // then
            assertThat(result).isEqualTo(0.42)
        }

        @Test
        fun `should return the default double when the setting does not exist`() {
            // given
            givenNothingStored()

            // when
            val result = testee.getOrDefault(id, 1.0)

            // then
            assertThat(result).isEqualTo(1.0)
        }

        @Test
        fun `should return the default double when the stored value is not a number`() {
            // given
            givenStored("not a number")

            // when
            val result = testee.getOrDefault(id, 1.0)

            // then
            assertThat(result).isEqualTo(1.0)
        }

        @Test
        fun `should parse the stored boolean when the setting exists`() {
            // given
            givenStored("false")

            // when
            val result = testee.getOrDefault(id, true)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `should return the default boolean when the setting does not exist`() {
            // given
            givenNothingStored()

            // when
            val result = testee.getOrDefault(id, true)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should return the default boolean when the stored value is not strictly a boolean`() {
            // given
            givenStored("TRUE")

            // when
            val result = testee.getOrDefault(id, false)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `should delete the setting when the value is null`() {
            // given

            // when
            testee.update(id, null)

            // then
            then(settingRepository).should().deleteById(id)
            then(settingRepository).shouldHaveNoMoreInteractions()
        }

        @Test
        fun `should update the existing setting with the string representation of the value`() {
            // given
            val existing = setting().name(id).value("old").build()
            given(settingRepository.findById(id)).willReturn(Optional.of(existing))

            // when
            testee.update(id, 12)

            // then
            then(settingRepository).should().save(existing)
            assertThat(existing.value).isEqualTo("12")
        }

        @Test
        fun `should create a new setting when it does not exist`() {
            // given
            val captor = argumentCaptor<Setting>()
            givenNothingStored()

            // when
            testee.update(id, true)

            // then
            then(settingRepository).should().save(captureValue(captor))
            assertThat(captor.value.name).isEqualTo(id)
            assertThat(captor.value.value).isEqualTo("true")
        }
    }
}
