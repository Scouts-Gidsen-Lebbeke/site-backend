package be.sgl.backend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ViolationFilterTest {

    private val strictModules = listOf("shared", "setting", "news")

    @Test
    fun `should flag violation when a strict module is the source`() {
        // given
        val message = STRICT_SOURCE_VIOLATION

        // when
        val offending = ViolationFilter.isOffending(message, strictModules)

        // then
        assertThat(offending).isTrue()
    }

    @Test
    fun `should flag cycle when it passes through a strict module`() {
        // given
        val message = CYCLE_THROUGH_STRICT_MODULE

        // when
        val offending = ViolationFilter.isOffending(message, strictModules)

        // then
        assertThat(offending).isTrue()
    }

    @Test
    fun `should tolerate violation when a legacy module depends on a strict module's internals`() {
        // given
        val message = LEGACY_SOURCE_VIOLATION

        // when
        val offending = ViolationFilter.isOffending(message, strictModules)

        // then
        assertThat(offending).isFalse()
    }

    @Test
    fun `should tolerate violation when only legacy modules are involved`() {
        // given
        val violation = LEGACY_ONLY_VIOLATION
        val cycle = LEGACY_ONLY_CYCLE

        // when
        val offending = listOf(violation, cycle).map { ViolationFilter.isOffending(it, strictModules) }

        // then
        assertThat(offending).containsOnly(false)
    }

    companion object {
        const val STRICT_SOURCE_VIOLATION =
            "Module 'news' depends on module 'entity' via be.sgl.backend.news.domain.TmpN -> be.sgl.backend.shared.address.Address. Allowed targets: shared."

        const val LEGACY_SOURCE_VIOLATION =
            "Module 'tmpa' depends on non-exposed type be.sgl.backend.news.domain.NewsItem within module 'news'!\n" +
                "Method <be.sgl.backend.tmpa.TmpA.f(be.sgl.backend.news.domain.NewsItem)> has return type <be.sgl.backend.news.domain.NewsItem> in (TmpA.kt:0)"

        const val CYCLE_THROUGH_STRICT_MODULE =
            "Cycle detected: Slice news -> \n" +
                "                Slice tmpc -> \n" +
                "                Slice news\n" +
                "  1. Dependencies of Slice news\n" +
                "    - Method <be.sgl.backend.news.domain.TmpN2.f(be.sgl.backend.tmpc.TmpC)> has parameter of type <be.sgl.backend.tmpc.TmpC> in (TmpN2.kt:0)\n" +
                "  2. Dependencies of Slice tmpc\n" +
                "    - Method <be.sgl.backend.tmpc.TmpC.f(be.sgl.backend.news.domain.NewsItem)> has parameter of type <be.sgl.backend.news.domain.NewsItem> in (TmpC.kt:0)"

        const val LEGACY_ONLY_VIOLATION =
            "Module 'controller' depends on non-exposed type be.sgl.backend.service.belcotax.BelcotaxService within module 'service'!\n" +
                "BelcotaxController declares constructor BelcotaxController(BelcotaxService) in (BelcotaxController.kt:0)"

        const val LEGACY_ONLY_CYCLE =
            "Cycle detected: Slice dto -> \n" +
                "                Slice service -> \n" +
                "                Slice dto\n" +
                "  1. Dependencies of Slice dto\n" +
                "    - Field <be.sgl.backend.dto.user.ExternalMember.syncState> has type <be.sgl.backend.service.user.sync.SyncState> in (ExternalMember.kt:0)"
    }
}
