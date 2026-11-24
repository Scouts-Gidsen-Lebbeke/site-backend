package be.sgl.backend.service.registrable

import be.sgl.backend.entity.PayableTestMother.event
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class CalculatePriceFromAdditionalDataTest {

    private val testee = CalculatePriceFromAdditionalData(JsonMapper())

    @Test
    fun `should return zero when the registrable has no form rule`() {
        // given
        val event = event().build()

        // when
        val price = testee.execute(event, """{"count":"4"}""")

        // then
        assertThat(price).isZero()
    }

    @Test
    fun `should return zero when no additional data is given`() {
        // given
        val event = event().additionalFormRule("count * 2.5").build()

        // when
        val price = testee.execute(event, null)

        // then
        assertThat(price).isZero()
    }

    @Test
    fun `should evaluate the rule with numeric strings converted to numbers`() {
        // given
        val event = event().additionalFormRule("count * 2.5").build()

        // when
        val price = testee.execute(event, """{"count":"4"}""")

        // then
        assertThat(price).isEqualTo(10.0)
    }

    @Test
    fun `should evaluate the rule with text values when values are not numeric`() {
        // given
        val event = event().additionalFormRule("""size = "XL" ? 5 : 0""").build()

        // when
        val price = testee.execute(event, """{"size":"XL"}""")

        // then
        assertThat(price).isEqualTo(5.0)
    }

    @Test
    fun `should clamp the price to zero when the rule evaluates to a negative amount`() {
        // given
        val event = event().additionalFormRule("count * -2").build()

        // when
        val price = testee.execute(event, """{"count":"4"}""")

        // then
        assertThat(price).isZero()
    }

    @Test
    fun `should return zero when the rule does not evaluate to a number`() {
        // given
        val event = event().additionalFormRule("size").build()

        // when
        val price = testee.execute(event, """{"size":"XL"}""")

        // then
        assertThat(price).isZero()
    }

    @Test
    fun `should return zero when the rule refers to a missing field`() {
        // given
        val event = event().additionalFormRule("missing * 2").build()

        // when
        val price = testee.execute(event, """{"count":"4"}""")

        // then
        assertThat(price).isZero()
    }

    @Test
    fun `should evaluate the rule when the additional data contains json numbers`() {
        // given
        val event = event().additionalFormRule("count * 2.5").build()

        // when
        val price = testee.execute(event, """{"count":4}""")

        // then
        assertThat(price).isEqualTo(10.0)
    }
}
