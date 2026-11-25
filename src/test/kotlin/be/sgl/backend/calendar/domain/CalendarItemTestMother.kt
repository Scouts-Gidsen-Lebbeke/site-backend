package be.sgl.backend.calendar.domain

import be.sgl.backend.shared.address.Address
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

object CalendarItemTestMother {

    fun calendarItem() = CalendarItemBuilder()

    class CalendarItemBuilder {
        private var id: Int? = 1
        private var start = LocalDateTime.of(2025, 9, 7, 14, 0)
        private var end = LocalDateTime.of(2025, 9, 7, 17, 0)
        private var title = "calendarItemTitle"
        private var content = "calendarItemContent"
        private var image: String? = null
        private val calendars = mutableListOf<Calendar>()
        private var closed = false
        private var address: Address? = null

        fun id(id: Int?) = apply { this.id = id }

        fun start(start: LocalDateTime) = apply { this.start = start }

        fun end(end: LocalDateTime) = apply { this.end = end }

        fun title(title: String) = apply { this.title = title }

        fun content(content: String) = apply { this.content = content }

        fun image(image: String) = apply { this.image = image }

        fun calendars(vararg calendars: Calendar) = apply { this.calendars.addAll(calendars) }

        fun closed() = apply { this.closed = true }

        fun address(address: Address) = apply { this.address = address }

        fun build(): CalendarItem {
            val item = CalendarItem()
            ReflectionTestUtils.setField(item, "id", id)
            item.start = start
            item.end = end
            item.title = title
            item.content = content
            item.image = image
            item.closed = closed
            item.address = address
            calendars.forEach {
                item.calendars.add(it)
                it.items.add(item)
            }
            return item
        }
    }
}
