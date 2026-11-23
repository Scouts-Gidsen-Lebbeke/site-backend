package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class RegistrableStatusTest {

    private val now = LocalDateTime.now()

    @Test
    fun `should be cancelled when the registrable is cancelled regardless of its dates`() {
        // given
        val activity = activity().open(now.plusDays(1)).closed(now.plusDays(2)).start(now.plusDays(3))
            .end(now.plusDays(4)).cancelled(true).build()

        // when
        val status = activity.getStatus()

        // then
        assertThat(status).isEqualTo(RegistrableStatus.CANCELLED)
    }

    @Test
    fun `should be not yet open when the opening date is in the future`() {
        // given
        val activity = activity().open(now.plusDays(1)).closed(now.plusDays(2)).start(now.plusDays(3))
            .end(now.plusDays(4)).build()

        // when
        val status = activity.getStatus()

        // then
        assertThat(status).isEqualTo(RegistrableStatus.NOT_YET_OPEN)
    }

    @Test
    fun `should have opened registrations when between opening and closing date`() {
        // given
        val activity = activity().open(now.minusDays(1)).closed(now.plusDays(2)).start(now.plusDays(3))
            .end(now.plusDays(4)).build()

        // when
        val status = activity.getStatus()

        // then
        assertThat(status).isEqualTo(RegistrableStatus.REGISTRATIONS_OPENED)
    }

    @Test
    fun `should have completed registrations when between closing and start date`() {
        // given
        val activity = activity().open(now.minusDays(2)).closed(now.minusDays(1)).start(now.plusDays(3))
            .end(now.plusDays(4)).build()

        // when
        val status = activity.getStatus()

        // then
        assertThat(status).isEqualTo(RegistrableStatus.REGISTRATIONS_COMPLETED)
    }

    @Test
    fun `should be started when between start and end date`() {
        // given
        val activity = activity().open(now.minusDays(3)).closed(now.minusDays(2)).start(now.minusDays(1))
            .end(now.plusDays(4)).build()

        // when
        val status = activity.getStatus()

        // then
        assertThat(status).isEqualTo(RegistrableStatus.STARTED)
    }

    @Test
    fun `should be completed when the end date has passed`() {
        // given
        val activity = activity().open(now.minusDays(4)).closed(now.minusDays(3)).start(now.minusDays(2))
            .end(now.minusDays(1)).build()

        // when
        val status = activity.getStatus()

        // then
        assertThat(status).isEqualTo(RegistrableStatus.COMPLETED)
    }
}
