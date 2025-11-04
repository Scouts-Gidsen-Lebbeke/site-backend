package be.sgl.backend.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Base64

class TextUtilsTest {

    @Test
    fun `nullIfBlank should return null for blank strings`() {
        assertNull("".nullIfBlank())
        assertNull("   ".nullIfBlank())
        assertNull(null.nullIfBlank())
    }

    @Test
    fun `nullIfBlank should return value for non-blank strings`() {
        assertEquals("test", "test".nullIfBlank())
        assertEquals("hello world", "hello world".nullIfBlank())
    }

    @Test
    fun `base64Encoded should encode string correctly`() {
        val input = "Hello World"
        val expected = Base64.getEncoder().encodeToString(input.toByteArray())
        assertEquals(expected, input.base64Encoded())
    }

    @Test
    fun `base64Encoded should handle empty string`() {
        val input = ""
        val expected = Base64.getEncoder().encodeToString(input.toByteArray())
        assertEquals(expected, input.base64Encoded())
    }

    @Test
    fun `belgian date format for LocalDate should be dd-MM-yyyy`() {
        val date = LocalDate.of(2023, 12, 25)
        assertEquals("25/12/2023", date.belgian())
    }

    @Test
    fun `belgian date format for LocalDateTime should be dd-MM-yyyy`() {
        val dateTime = LocalDateTime.of(2023, 12, 25, 15, 30, 45)
        assertEquals("25/12/2023", dateTime.belgian())
    }

    @Test
    fun `pricePrecision should format double with 2 decimals`() {
        assertEquals("10.50", 10.5.pricePrecision())
        assertEquals("10.99", 10.99.pricePrecision())
        assertEquals("10.00", 10.0.pricePrecision())
    }

    @Test
    fun `pricePrecision should return null for null input`() {
        val value: Double? = null
        assertNull(value.pricePrecision())
    }

    @Test
    fun `reducePrice should calculate reduced price correctly`() {
        assertEquals(50.0, 100.0.reducePrice(2.0))
        assertEquals(33.33, 100.0.reducePrice(3.0))
        assertEquals(25.0, 50.0.reducePrice(2.0))
    }

    @Test
    fun `reducePrice should round to 2 decimal places`() {
        val result = 10.0.reducePrice(3.0)
        assertEquals(3.33, result)
    }
}
