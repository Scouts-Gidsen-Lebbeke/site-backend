package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ActivityRegistrationTest {

    @Test
    fun `should count one day when start and end are on the same day`() {
        // given
        val registration = activityRegistration()
            .start(LocalDateTime.of(2025, 7, 1, 9, 0))
            .end(LocalDateTime.of(2025, 7, 1, 17, 0))
            .build()

        // when
        val days = registration.calculateDays()

        // then
        assertThat(days).isEqualTo(1)
    }

    @Test
    fun `should count both start and end day when the activity spans multiple days`() {
        // given
        val registration = activityRegistration()
            .start(LocalDateTime.of(2025, 7, 21, 18, 0))
            .end(LocalDateTime.of(2025, 7, 31, 10, 0))
            .build()

        // when
        val days = registration.calculateDays()

        // then
        assertThat(days).isEqualTo(11)
    }

    @Test
    fun `should count calendar days when the activity spans midnight in less than 24 hours`() {
        // given
        val registration = activityRegistration()
            .start(LocalDateTime.of(2025, 12, 31, 20, 0))
            .end(LocalDateTime.of(2026, 1, 1, 2, 0))
            .build()

        // when
        val days = registration.calculateDays()

        // then
        assertThat(days).isEqualTo(2)
    }
}
