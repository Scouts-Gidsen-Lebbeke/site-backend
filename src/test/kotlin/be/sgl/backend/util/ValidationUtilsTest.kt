package be.sgl.backend.util

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ValidationUtilsTest {

    private lateinit var validator: Validator

    @BeforeEach
    fun setup() {
        validator = Validation.buildDefaultValidatorFactory().validator
    }

    // PhoneNumber validation tests
    @Test
    fun `PhoneNumberValidator should accept valid 9 digit phone numbers`() {
        val phoneValidator = PhoneNumberValidator()
        assertTrue(phoneValidator.isValid("123456789", null))
        assertTrue(phoneValidator.isValid("987654321", null))
    }

    @Test
    fun `PhoneNumberValidator should accept valid 10 digit phone numbers`() {
        val phoneValidator = PhoneNumberValidator()
        assertTrue(phoneValidator.isValid("0123456789", null))
        assertTrue(phoneValidator.isValid("0987654321", null))
    }

    @Test
    fun `PhoneNumberValidator should reject invalid phone numbers`() {
        val phoneValidator = PhoneNumberValidator()
        assertFalse(phoneValidator.isValid("12345", null))
        assertFalse(phoneValidator.isValid("12345678901", null))
        assertFalse(phoneValidator.isValid("abcdefghi", null))
        assertFalse(phoneValidator.isValid("123-456-789", null))
    }

    @Test
    fun `PhoneNumberValidator should accept null or blank values`() {
        val phoneValidator = PhoneNumberValidator()
        assertTrue(phoneValidator.isValid(null, null))
        assertTrue(phoneValidator.isValid("", null))
        assertTrue(phoneValidator.isValid("   ", null))
    }

    // NIS validation tests
    @Test
    fun `NisValidator should accept valid NIS numbers`() {
        val nisValidator = NisValidator()
        // Valid NIS: 85073003328 (example)
        assertTrue(nisValidator.isValid("85073003328", null))
    }

    @Test
    fun `NisValidator should reject invalid NIS numbers`() {
        val nisValidator = NisValidator()
        assertFalse(nisValidator.isValid("12345678901", null))
        assertFalse(nisValidator.isValid("00000000000", null))
    }

    @Test
    fun `NisValidator should reject NIS with wrong length`() {
        val nisValidator = NisValidator()
        assertFalse(nisValidator.isValid("123456789", null))
        assertFalse(nisValidator.isValid("123456789012", null))
    }

    @Test
    fun `NisValidator should reject non-numeric NIS`() {
        val nisValidator = NisValidator()
        assertFalse(nisValidator.isValid("abcdefghijk", null))
        assertFalse(nisValidator.isValid("123-456-789", null))
    }

    @Test
    fun `NisValidator should accept null values`() {
        val nisValidator = NisValidator()
        assertTrue(nisValidator.isValid(null, null))
    }

    // CountryCode validation tests
    @Test
    fun `CountryCodeValidator should accept valid ISO country codes`() {
        val countryValidator = CountryCodeValidator()
        assertTrue(countryValidator.isValid("BE", null))
        assertTrue(countryValidator.isValid("NL", null))
        assertTrue(countryValidator.isValid("FR", null))
        assertTrue(countryValidator.isValid("US", null))
        assertTrue(countryValidator.isValid("be", null)) // lowercase should be accepted
    }

    @Test
    fun `CountryCodeValidator should reject invalid country codes`() {
        val countryValidator = CountryCodeValidator()
        assertFalse(countryValidator.isValid("XX", null))
        assertFalse(countryValidator.isValid("ZZZ", null))
        assertFalse(countryValidator.isValid("123", null))
    }

    @Test
    fun `CountryCodeValidator should accept null values`() {
        val countryValidator = CountryCodeValidator()
        assertTrue(countryValidator.isValid(null, null))
    }

    // KBO validation tests
    @Test
    fun `KboValidator should accept valid KBO numbers`() {
        val kboValidator = KboValidator()
        assertTrue(kboValidator.isValid("0123456789", null))
        assertTrue(kboValidator.isValid("0987654321", null))
    }

    @Test
    fun `KboValidator should reject KBO numbers not starting with 0`() {
        val kboValidator = KboValidator()
        assertFalse(kboValidator.isValid("1123456789", null))
        assertFalse(kboValidator.isValid("9987654321", null))
    }

    @Test
    fun `KboValidator should reject invalid KBO format`() {
        val kboValidator = KboValidator()
        assertFalse(kboValidator.isValid("012345678", null)) // too short
        assertFalse(kboValidator.isValid("01234567890", null)) // too long
        assertFalse(kboValidator.isValid("012345678a", null)) // contains letter
    }

    @Test
    fun `KboValidator should accept null or blank values`() {
        val kboValidator = KboValidator()
        assertTrue(kboValidator.isValid(null, null))
        assertTrue(kboValidator.isValid("", null))
        assertTrue(kboValidator.isValid("   ", null))
    }

    // StartEndTime validation tests
    @Test
    fun `StartEndTimeValidator should accept valid time ranges`() {
        val validator = StartEndTimeValidator()
        val validRange = object : StartEndTime {
            override val start = LocalDateTime.of(2023, 1, 1, 10, 0)
            override val end = LocalDateTime.of(2023, 1, 1, 12, 0)
        }
        assertTrue(validator.isValid(validRange, null))
    }

    @Test
    fun `StartEndTimeValidator should reject end before start`() {
        val validator = StartEndTimeValidator()
        val invalidRange = object : StartEndTime {
            override val start = LocalDateTime.of(2023, 1, 1, 12, 0)
            override val end = LocalDateTime.of(2023, 1, 1, 10, 0)
        }
        assertFalse(validator.isValid(invalidRange, null))
    }

    @Test
    fun `StartEndTimeValidator should accept null values`() {
        val validator = StartEndTimeValidator()
        assertTrue(validator.isValid(null, null))
    }

    @Test
    fun `StartEndTimeValidator should accept null start or end`() {
        val validator = StartEndTimeValidator()
        val nullStart = object : StartEndTime {
            override val start: LocalDateTime? = null
            override val end = LocalDateTime.of(2023, 1, 1, 12, 0)
        }
        val nullEnd = object : StartEndTime {
            override val start = LocalDateTime.of(2023, 1, 1, 10, 0)
            override val end: LocalDateTime? = null
        }
        assertTrue(validator.isValid(nullStart, null))
        assertTrue(validator.isValid(nullEnd, null))
    }

    // StartEndDate validation tests
    @Test
    fun `StartEndDateValidator should accept valid date ranges`() {
        val validator = StartEndDateValidator()
        val validRange = object : StartEndDate {
            override val start = LocalDate.of(2023, 1, 1)
            override val end = LocalDate.of(2023, 12, 31)
        }
        assertTrue(validator.isValid(validRange, null))
    }

    @Test
    fun `StartEndDateValidator should reject end before start`() {
        val validator = StartEndDateValidator()
        val invalidRange = object : StartEndDate {
            override val start = LocalDate.of(2023, 12, 31)
            override val end = LocalDate.of(2023, 1, 1)
        }
        assertFalse(validator.isValid(invalidRange, null))
    }

    @Test
    fun `StartEndDateValidator should accept null values`() {
        val validator = StartEndDateValidator()
        assertTrue(validator.isValid(null, null))
    }

    @Test
    fun `StartEndDateValidator should accept null start or end`() {
        val validator = StartEndDateValidator()
        val nullStart = object : StartEndDate {
            override val start: LocalDate? = null
            override val end = LocalDate.of(2023, 12, 31)
        }
        val nullEnd = object : StartEndDate {
            override val start = LocalDate.of(2023, 1, 1)
            override val end: LocalDate? = null
        }
        assertTrue(validator.isValid(nullStart, null))
        assertTrue(validator.isValid(nullEnd, null))
    }
}
