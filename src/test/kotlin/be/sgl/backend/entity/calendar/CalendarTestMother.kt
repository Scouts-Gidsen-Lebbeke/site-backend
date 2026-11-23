package be.sgl.backend.entity.calendar

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchTestMother

object CalendarTestMother {

    fun calendar() = CalendarBuilder()

    class CalendarBuilder {
        private var id: Int? = 1
        private var period = CalendarPeriodTestMother.calendarPeriod().build()
        private var branch = BranchTestMother.branch().build()
        private var intro: String? = null
        private var outro: String? = null
        private val items = mutableListOf<CalendarItem>()

        fun id(id: Int?) = apply { this.id = id }

        fun period(period: CalendarPeriod) = apply { this.period = period }

        fun branch(branch: Branch) = apply { this.branch = branch }

        fun intro(intro: String) = apply { this.intro = intro }

        fun outro(outro: String) = apply { this.outro = outro }

        fun items(vararg items: CalendarItem) = apply { this.items.addAll(items) }

        /** Builds the calendar and links it to each of its items, as the owning side of the relation. */
        fun build(): Calendar {
            val calendar = Calendar(period, branch)
            calendar.id = id
            calendar.intro = intro
            calendar.outro = outro
            items.forEach {
                calendar.items.add(it)
                if (calendar !in it.calendars) {
                    it.calendars.add(calendar)
                }
            }
            return calendar
        }
    }
}
