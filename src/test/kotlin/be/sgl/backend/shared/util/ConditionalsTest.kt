package be.sgl.backend.shared.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotationMetadata
import org.springframework.mock.env.MockEnvironment

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT) // not every scenario reaches the environment
class ConditionalsTest {

    @Mock
    lateinit var context: ConditionContext

    private val testee = WhenNotBlank.WhenNotBlankCondition()
    private val environment = MockEnvironment()

    @WhenNotBlank("some.property")
    class WhenSet

    @WhenNotBlank("some.property", not = true)
    class WhenNotSet

    @ForExternalOrganization
    class External

    @ForInternalOrganization
    class Internal

    class NotAnnotated

    private fun matches(type: Class<*>, property: String?, value: String?): Boolean {
        if (property != null && value != null) environment.setProperty(property, value)
        given(context.environment).willReturn(environment)
        return testee.matches(context, AnnotationMetadata.introspect(type))
    }

    @ParameterizedTest
    @CsvSource(
        "value, true",
        "'', false",
        "'   ', false",
    )
    fun `should match the regular condition only when the property has a non blank value`(value: String, expected: Boolean) {
        // given

        // when
        val result = matches(WhenSet::class.java, "some.property", value)

        // then
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should not match the regular condition when the property is missing`() {
        // given

        // when
        val result = matches(WhenSet::class.java, null, null)

        // then
        assertThat(result).isFalse()
    }

    @ParameterizedTest
    @CsvSource(
        "value, false",
        "'', true",
        "'   ', true",
    )
    fun `should match the negated condition only when the property is blank`(value: String, expected: Boolean) {
        // given

        // when
        val result = matches(WhenNotSet::class.java, "some.property", value)

        // then
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should match the negated condition when the property is missing`() {
        // given

        // when
        val result = matches(WhenNotSet::class.java, null, null)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `should match external organization and not internal when external id is set`() {
        // given
        environment.setProperty("organization.external.id", "A1234G")

        // when
        val external = matches(External::class.java, null, null)
        val internal = matches(Internal::class.java, null, null)

        // then
        assertThat(external).isTrue()
        assertThat(internal).isFalse()
    }

    @Test
    fun `should match internal organization and not external when external id is missing`() {
        // given

        // when
        val external = matches(External::class.java, null, null)
        val internal = matches(Internal::class.java, null, null)

        // then
        assertThat(external).isFalse()
        assertThat(internal).isTrue()
    }

    @Test
    fun `should not match when the type is not annotated`() {
        // given

        // when
        val result = matches(NotAnnotated::class.java, null, null)

        // then
        assertThat(result).isFalse()
    }
}
