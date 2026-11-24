package be.sgl.backend.util

import jakarta.validation.ConstraintValidatorContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import java.time.LocalDate
import java.time.LocalDateTime

class ValidationUtilsTest {

    private val context: ConstraintValidatorContext = mock(ConstraintValidatorContext::class.java)

    @Nested
    inner class PhoneNumberValidatorTest {

        private val testee = PhoneNumberValidator()

        @ParameterizedTest
        @ValueSource(strings = ["052123456", "0471234567", "", "   "])
        fun `should accept phone number when it has 9 or 10 digits or is blank`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @ParameterizedTest
        @NullSource
        fun `should accept phone number when it is null`(value: String?) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @ParameterizedTest
        @ValueSource(strings = ["05212345", "04712345678", "+32471234567", "0471 23 45 67", "047123456a", " 0471234567"])
        fun `should reject phone number when it is not 9 or 10 plain digits`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }
    }

    @Nested
    inner class NisValidatorTest {

        private val testee = NisValidator()

        @ParameterizedTest
        @CsvSource(
            "85031512369, born before 2000",
            "15031512383, born after 2010",
            "05031512338, born in 1905",
        )
        fun `should accept nis when checksum is valid`(value: String, case: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).describedAs(case).isTrue()
        }

        @ParameterizedTest
        @NullSource
        fun `should accept nis when it is null`(value: String?) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @ParameterizedTest
        @ValueSource(strings = ["85031512368", "15031512384", "8503151236", "850315123690", "85.03.15-123.69", "8503151236a", "", " 85031512369"])
        fun `should reject nis when checksum or format is invalid`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }

        @Test
        fun `should accept nis when person is born between 2000 and 2009`() {
            // given
            val nisBornIn2005 = "05031512367"

            // when
            val valid = testee.isValid(nisBornIn2005, context)

            // then
            assertThat(valid).isTrue()
        }
    }

    @Nested
    inner class CountryCodeValidatorTest {

        private val testee = CountryCodeValidator()

        @ParameterizedTest
        @ValueSource(strings = ["BE", "NL", "be", "GB"])
        fun `should accept country when it is an iso country code`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @ParameterizedTest
        @NullSource
        fun `should accept country when it is null`(value: String?) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @ParameterizedTest
        @ValueSource(strings = ["UK", "BEL", "XX", "", " BE"])
        fun `should reject country when it is not an iso country code`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }
    }

    @Nested
    inner class KboValidatorTest {

        private val testee = KboValidator()

        @ParameterizedTest
        @ValueSource(strings = ["0417035068", "", "  "])
        fun `should accept kbo when it starts with 0 and has 10 digits or is blank`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @ParameterizedTest
        @ValueSource(strings = ["417035068", "04170350681", "0417.035.068", "BE0417035068", "041703506a"])
        fun `should reject kbo when format is invalid`(value: String) {
            // given

            // when
            val valid = testee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }

        @Test
        fun `should reject kbo when checksum is invalid`() {
            // given
            val invalidChecksum = "0417035069"

            // when
            val valid = testee.isValid(invalidChecksum, context)

            // then
            assertThat(valid).isFalse()
        }

        @Test
        fun `should accept kbo when it starts with 1`() {
            // given
            val newRangeKbo = "1001234592"

            // when
            val valid = testee.isValid(newRangeKbo, context)

            // then
            assertThat(valid).isTrue()
        }
    }

    @Nested
    inner class StartBeforeEndTest {

        private val timeTestee = StartEndTimeValidator()
        private val dateTestee = StartEndDateValidator()
        private val start = LocalDateTime.of(2025, 7, 1, 10, 0)

        private fun times(start: LocalDateTime?, end: LocalDateTime?) = object : StartEndTime {
            override val start = start
            override val end = end
        }

        private fun dates(start: LocalDate?, end: LocalDate?) = object : StartEndDate {
            override val start = start
            override val end = end
        }

        @Test
        fun `should accept times when start is before end`() {
            // given
            val value = times(start, start.plusMinutes(1))

            // when
            val valid = timeTestee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @Test
        fun `should reject times when start equals end`() {
            // given
            val value = times(start, start)

            // when
            val valid = timeTestee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }

        @Test
        fun `should reject times when start is after end`() {
            // given
            val value = times(start, start.minusDays(1))

            // when
            val valid = timeTestee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }

        @Test
        fun `should accept times when start or end is missing`() {
            // given
            val values = listOf(null, times(null, start), times(start, null))

            // when
            val results = values.map { timeTestee.isValid(it, context) }

            // then
            assertThat(results).containsOnly(true)
        }

        @Test
        fun `should accept dates when start is before end`() {
            // given
            val value = dates(start.toLocalDate(), start.toLocalDate().plusDays(1))

            // when
            val valid = dateTestee.isValid(value, context)

            // then
            assertThat(valid).isTrue()
        }

        @Test
        fun `should reject dates when start equals end`() {
            // given
            val value = dates(start.toLocalDate(), start.toLocalDate())

            // when
            val valid = dateTestee.isValid(value, context)

            // then
            assertThat(valid).isFalse()
        }

        @Test
        fun `should accept dates when start or end is missing`() {
            // given
            val values = listOf(null, dates(null, start.toLocalDate()), dates(start.toLocalDate(), null))

            // when
            val results = values.map { dateTestee.isValid(it, context) }

            // then
            assertThat(results).containsOnly(true)
        }
    }
}
