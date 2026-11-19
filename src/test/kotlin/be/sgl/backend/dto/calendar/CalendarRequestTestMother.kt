package be.sgl.backend.dto.calendar

import be.sgl.backend.dto.AddressDTO
import java.time.LocalDate
import java.time.LocalDateTime

object CalendarRequestTestMother {

    fun calendarPeriodRequest() = CreateOrUpdateCalendarPeriodRequestBuilder()

    fun calendarItemRequest() = CreateOrUpdateCalendarItemRequestBuilder()

    class CreateOrUpdateCalendarPeriodRequestBuilder {
        private var name: String? = "calendarPeriodName"
        private var start: LocalDate? = LocalDate.of(2025, 9, 1)
        private var end: LocalDate? = LocalDate.of(2025, 12, 31)

        fun name(name: String?) = apply { this.name = name }

        fun start(start: LocalDate?) = apply { this.start = start }

        fun end(end: LocalDate?) = apply { this.end = end }

        fun build() = CreateOrUpdateCalendarPeriodRequest(name, start, end)
    }

    class CreateOrUpdateCalendarItemRequestBuilder {
        private var start = LocalDateTime.of(2025, 9, 14, 14, 0)
        private var end = LocalDateTime.of(2025, 9, 14, 17, 0)
        private var title = "requestTitle"
        private var content = "requestContent"
        private var image: String? = null
        private var closed: Boolean? = null
        private var address: AddressDTO? = null
        private val calendars = mutableListOf<Int>()

        fun start(start: LocalDateTime) = apply { this.start = start }

        fun end(end: LocalDateTime) = apply { this.end = end }

        fun title(title: String) = apply { this.title = title }

        fun content(content: String) = apply { this.content = content }

        fun image(image: String?) = apply { this.image = image }

        fun closed() = apply { this.closed = true }

        fun address(address: AddressDTO) = apply { this.address = address }

        fun calendars(vararg calendars: Int) = apply { this.calendars.addAll(calendars.toList()) }

        fun build() = CreateOrUpdateCalendarItemRequest(start, end, title, content, image, closed, address, calendars)
    }
}
