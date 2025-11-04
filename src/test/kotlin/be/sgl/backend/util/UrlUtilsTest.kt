package be.sgl.backend.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class UrlUtilsTest {

    @Test
    fun `appendRequestParameters should add single parameter to URL without query string`() {
        val result = appendRequestParameters("https://example.com/api", "key" to "value")
        assertEquals("https://example.com/api?key=value", result)
    }

    @Test
    fun `appendRequestParameters should add multiple parameters to URL without query string`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "key1" to "value1",
            "key2" to "value2"
        )
        assertEquals("https://example.com/api?key1=value1&key2=value2", result)
    }

    @Test
    fun `appendRequestParameters should append to existing query string`() {
        val result = appendRequestParameters(
            "https://example.com/api?existing=param",
            "key" to "value"
        )
        assertEquals("https://example.com/api?existing=param&key=value", result)
    }

    @Test
    fun `appendRequestParameters should URL encode parameter names`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "key with spaces" to "value"
        )
        assertTrue(result.contains("key+with+spaces=value"))
    }

    @Test
    fun `appendRequestParameters should URL encode parameter values`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "key" to "value with spaces"
        )
        assertTrue(result.contains("key=value+with+spaces"))
    }

    @Test
    fun `appendRequestParameters should handle special characters`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "email" to "test@example.com"
        )
        assertTrue(result.contains("email=test%40example.com"))
    }

    @Test
    fun `appendRequestParameters should handle null values`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "key" to null
        )
        assertEquals("https://example.com/api?key=null", result)
    }

    @Test
    fun `appendRequestParameters should handle numeric values`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "count" to 42,
            "price" to 19.99
        )
        assertTrue(result.contains("count=42"))
        assertTrue(result.contains("price=19.99"))
    }

    @Test
    fun `appendRequestParameters should handle boolean values`() {
        val result = appendRequestParameters(
            "https://example.com/api",
            "active" to true,
            "deleted" to false
        )
        assertTrue(result.contains("active=true"))
        assertTrue(result.contains("deleted=false"))
    }
}
