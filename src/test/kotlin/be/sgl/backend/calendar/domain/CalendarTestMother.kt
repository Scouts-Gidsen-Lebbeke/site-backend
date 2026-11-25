package be.sgl.backend.calendar.domain

object CalendarTestMother {

    fun calendar() = CalendarBuilder()

    class CalendarBuilder {
        private var id: Int? = 1
        private var period = CalendarPeriodTestMother.calendarPeriod().build()
        private var branchId = 1
        private var intro: String? = null
        private var outro: String? = null
        private val items = mutableListOf<CalendarItem>()

        fun id(id: Int?) = apply { this.id = id }

        fun period(period: CalendarPeriod) = apply { this.period = period }

        fun branchId(branchId: Int) = apply { this.branchId = branchId }

        fun intro(intro: String) = apply { this.intro = intro }

        fun outro(outro: String) = apply { this.outro = outro }

        fun items(vararg items: CalendarItem) = apply { this.items.addAll(items) }

        fun build(): Calendar {
            val calendar = Calendar(period, branchId)
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
