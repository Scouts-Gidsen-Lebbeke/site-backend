package be.sgl.backend.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64

class TextUtilsTest {

    @Test
    fun `should return null when string is null, empty or blank`() {
        // given
        val values = listOf(null, "", "   ", "\t\n")

        // when
        val results = values.map { it.nullIfBlank() }

        // then
        assertThat(results).containsOnlyNulls()
    }

    @Test
    fun `should keep the string untouched when it is not blank`() {
        // given
        val value = " text "

        // when
        val result = value.nullIfBlank()

        // then
        assertThat(result).isEqualTo(value)
    }

    @Test
    fun `should encode string as base64 when encoding`() {
        // given
        val value = "user:pässword"

        // when
        val encoded = value.base64Encoded()

        // then
        assertThat(String(Base64.getDecoder().decode(encoded))).isEqualTo(value)
    }

    @Test
    fun `should format date in belgian format`() {
        // given
        val date = LocalDate.of(2025, 3, 7)

        // when
        val formatted = date.belgian()

        // then
        assertThat(formatted).isEqualTo("07/03/2025")
    }

    @Test
    fun `should format date time in belgian format without time`() {
        // given
        val dateTime = LocalDateTime.of(2025, 12, 31, 23, 59)

        // when
        val formatted = dateTime.belgian()

        // then
        assertThat(formatted).isEqualTo("31/12/2025")
    }

    @ParameterizedTest
    @CsvSource("1.5,'1,50'", "0.0,'0,00'", "1234.567,'1234,57'", "-2.0,'-2,00'")
    fun `should format price with two decimals and belgian decimal separator`(price: Double, expected: String) {
        // given

        // when
        val formatted = price.pricePrecision()

        // then
        assertThat(formatted).isEqualTo(expected)
    }

    @Test
    fun `should return null price precision when price is null`() {
        // given
        val price: Double? = null

        // when
        val formatted = price.pricePrecision()

        // then
        assertThat(formatted).isNull()
    }

    @ParameterizedTest
    @CsvSource("10.0,4.0,2.5", "10.0,3.0,3.33", "20.0,3.0,6.67", "0.0,2.0,0.0")
    fun `should divide price by factor and round half up to cents`(price: Double, factor: Double, expected: Double) {
        // given

        // when
        val reduced = price.reducePrice(factor)

        // then
        assertThat(reduced).isEqualTo(expected)
    }
}
