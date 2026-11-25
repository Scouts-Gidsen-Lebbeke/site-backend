package be.sgl.backend.shared.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UrlUtilsTest {

    @Test
    fun `should start a query string when url has no parameters yet`() {
        // given
        val url = "https://site.be/pay"

        // when
        val result = appendRequestParameters(url, "id" to 5, "type" to "activity")

        // then
        assertThat(result).isEqualTo("https://site.be/pay?id=5&type=activity")
    }

    @Test
    fun `should append to the existing query string when url already has parameters`() {
        // given
        val url = "https://site.be/pay?lang=nl"

        // when
        val result = appendRequestParameters(url, "id" to 5)

        // then
        assertThat(result).isEqualTo("https://site.be/pay?lang=nl&id=5")
    }

    @Test
    fun `should url encode names and values when they contain special characters`() {
        // given
        val url = "https://site.be"

        // when
        val result = appendRequestParameters(url, "redirect url" to "https://a.be/?x=1&y=é")

        // then
        assertThat(result).isEqualTo("https://site.be?redirect+url=https%3A%2F%2Fa.be%2F%3Fx%3D1%26y%3D%C3%A9")
    }

    @Test
    fun `should write the literal null when a parameter value is null`() {
        // given
        val url = "https://site.be"

        // when
        val result = appendRequestParameters(url, "id" to null)

        // then
        assertThat(result).isEqualTo("https://site.be?id=null")
    }

    @Test
    fun `should leave a dangling question mark when no parameters are given`() {
        // given
        val url = "https://site.be"

        // when
        val result = appendRequestParameters(url)

        // then
        assertThat(result).isEqualTo("https://site.be?")
    }
}
