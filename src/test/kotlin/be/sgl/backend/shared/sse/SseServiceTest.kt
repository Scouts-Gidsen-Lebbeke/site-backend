package be.sgl.backend.shared.sse

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class SseServiceTest {

    private val testee = SseService()

    private fun awaitRemoved(emitterId: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (testee.getEmitter(emitterId) != null && System.nanoTime() < deadline) {
            Thread.onSpinWait()
        }
    }

    @Test
    fun `should expose the emitter while the task is running`() {
        // given
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val received = AtomicReference<SseEmitter>()

        // when
        val emitterId = testee.schedule { emitter ->
            received.set(emitter)
            started.countDown()
            release.await(5, TimeUnit.SECONDS)
        }

        // then
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue()
        assertThat(testee.getEmitter(emitterId)).isSameAs(received.get())
        release.countDown()
    }

    @Test
    fun `should remove the emitter when the task completes`() {
        // given
        val finished = CountDownLatch(1)

        // when
        val emitterId = testee.schedule { finished.countDown() }

        // then
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue()
        awaitRemoved(emitterId)
        assertThat(testee.getEmitter(emitterId)).isNull()
    }

    @Test
    fun `should remove the emitter when the task fails`() {
        // given
        val failed = CountDownLatch(1)

        // when
        val emitterId = testee.schedule {
            failed.countDown()
            throw IllegalStateException("boom")
        }

        // then
        assertThat(failed.await(5, TimeUnit.SECONDS)).isTrue()
        awaitRemoved(emitterId)
        assertThat(testee.getEmitter(emitterId)).isNull()
    }

    @Test
    fun `should return a unique id per scheduled task`() {
        // given

        // when
        val ids = (1..5).map { testee.schedule { } }

        // then
        assertThat(ids).doesNotHaveDuplicates()
    }

    @Test
    fun `should return null when the emitter id is unknown`() {
        // given

        // when
        val emitter = testee.getEmitter("unknown")

        // then
        assertThat(emitter).isNull()
    }
}
