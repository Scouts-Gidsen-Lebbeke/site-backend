package be.sgl.backend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

    private val modules = ApplicationModules.of(SiteBackendApplication::class.java)

    @Test
    fun `should know every strict module when listing application modules`() {
        // given
        val names = modules.map { it.identifier.toString() }

        // when
        val unknown = STRICT_MODULES - names.toSet()

        // then
        assertThat(unknown).isEmpty()
    }

    @Test
    fun `should report no violations when they involve a strict module`() {
        // given
        val messages = modules.detectViolations().messages

        // when
        val offending = messages.filter { message -> STRICT_MODULES.any { message.involves(it) } }

        // then
        assertThat(offending).withFailMessage(offending.joinToString("\n\n")).isEmpty()
    }

    private fun String.involves(module: String) =
        contains("'$module'") || contains("Slice $module")

    companion object {
        val STRICT_MODULES = listOf("shared")
    }
}
