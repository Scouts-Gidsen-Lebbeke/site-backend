package be.sgl.backend.calendar.domain

import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate

object CalendarPeriodTestMother {

    fun calendarPeriod() = CalendarPeriodBuilder()

    class CalendarPeriodBuilder {
        private var id: Int? = 1
        private var name = "calendarPeriodName"
        private var start = LocalDate.of(2025, 9, 1)
        private var end = LocalDate.of(2025, 12, 31)

        fun id(id: Int?) = apply { this.id = id }

        fun name(name: String) = apply { this.name = name }

        fun start(start: LocalDate) = apply { this.start = start }

        fun end(end: LocalDate) = apply { this.end = end }

        fun build(): CalendarPeriod {
            val period = CalendarPeriod()
            ReflectionTestUtils.setField(period, "id", id)
            period.name = name
            period.start = start
            period.end = end
            return period
        }
    }
}
