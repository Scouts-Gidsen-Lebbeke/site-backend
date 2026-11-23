package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.calendar.CalendarTestMother.calendar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CalendarItemTest {

    @Test
    fun `should create an open item from 14h to 17h when a default item is requested for a sunday`() {
        // given
        val sunday = LocalDate.of(2025, 9, 7)
        val calendar = calendar().build()

        // when
        val item = CalendarItem.defaultItem(sunday, calendar)

        // then
        assertThat(item.start).isEqualTo(sunday.atTime(14, 0))
        assertThat(item.end).isEqualTo(sunday.atTime(17, 0))
        assertThat(item.title).isEqualTo("Nog in te vullen")
        assertThat(item.content).isEqualTo("Nog in te vullen")
        assertThat(item.calendars).containsExactly(calendar)
        assertThat(item.closed).isFalse()
    }
}
