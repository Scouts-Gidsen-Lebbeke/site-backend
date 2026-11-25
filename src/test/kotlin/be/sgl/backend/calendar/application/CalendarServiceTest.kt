package be.sgl.backend.calendar.application

import be.sgl.backend.calendar.web.CalendarItemDTO
import be.sgl.backend.organization.BranchApi
import be.sgl.backend.organization.BranchSummary
import be.sgl.backend.calendar.web.CalendarRequestTestMother.calendarItemRequest
import be.sgl.backend.calendar.web.UpdateCalendarRequest
import be.sgl.backend.calendar.domain.CalendarItem
import be.sgl.backend.calendar.domain.CalendarItemTestMother.calendarItem
import be.sgl.backend.calendar.domain.CalendarPeriodTestMother.calendarPeriod
import be.sgl.backend.calendar.domain.CalendarTestMother.calendar
import be.sgl.backend.shared.exception.CalendarItemNotFoundException
import be.sgl.backend.shared.exception.CalendarNotFoundException
import be.sgl.backend.shared.exception.CalendarPeriodNotFoundException
import be.sgl.backend.calendar.domain.CalendarItemRepository
import be.sgl.backend.calendar.domain.CalendarPeriodRepository
import be.sgl.backend.calendar.domain.CalendarRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.CALENDAR_ITEMS
import be.sgl.backend.shared.image.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CalendarServiceTest {

    @Mock
    lateinit var imageService: ImageService
    @Mock
    lateinit var branchApi: BranchApi
    @Mock
    lateinit var periodRepository: CalendarPeriodRepository
    @Mock
    lateinit var calendarRepository: CalendarRepository
    @Mock
    lateinit var itemRepository: CalendarItemRepository
    @Mock
    lateinit var entityManager: EntityManager
    @InjectMocks
    lateinit var testee: CalendarService

    private val id = 1
    private val defaultTitle = "Nog in te vullen"
    private val summary = BranchSummary(1, "branchName", "branch.jpg")

    private fun givenSummaries(vararg summaries: BranchSummary) {
        given(branchApi.findSummaries(summaries.map(BranchSummary::id).toSet())).willReturn(summaries.associateBy(BranchSummary::id))
    }

    @Test
    fun `should give each calendar its own branch summary when listing current calendars`() {
        // given
        val kapoenen = BranchSummary(1, "Kapoenen", "kapoenen.png")
        val welpen = BranchSummary(2, "Welpen", "welpen.png")
        given(calendarRepository.getCurrentCalendars()).willReturn(listOf(
            calendar().id(1).branchId(kapoenen.id).build(),
            calendar().id(2).branchId(welpen.id).build()
        ))
        givenSummaries(kapoenen, welpen)

        // when
        val result = testee.getCurrentCalendars()

        // then
        assertThat(result).extracting<Any> { it.branch }.containsExactly(kapoenen, welpen)
        then(branchApi).should().findSummaries(setOf(kapoenen.id, welpen.id))
        then(branchApi).shouldHaveNoMoreInteractions()
    }

    @Test
    fun `should throw when listing the calendars of an unknown period`() {
        // given
        given(periodRepository.findById(id)).willReturn(Optional.empty())

        // when / then
        assertThatThrownBy { testee.getCalendarsByPeriod(id) }
            .isInstanceOf(CalendarPeriodNotFoundException::class.java)
    }

    @Nested
    inner class GetCalendar {

        private val firstSunday = LocalDate.of(2025, 9, 7)
        private val secondSunday = LocalDate.of(2025, 9, 14)
        private val thirdSunday = LocalDate.of(2025, 9, 21)

        private fun itemOn(day: LocalDate, itemId: Int = 1) =
            calendarItem().id(itemId).start(day.atTime(10, 0)).end(day.atTime(12, 0)).title("real item").build()

        private fun startsOf(items: List<CalendarItemDTO>) = items.map(CalendarItemDTO::start)

        @Test
        fun `should throw when the calendar does not exist`() {
            // given
            given(calendarRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getCalendarDTOById(id, false) }
                .isInstanceOf(CalendarNotFoundException::class.java)
                .hasMessage("Calendar not found")
        }

        @Test
        fun `should return the items sorted by start without defaults when defaults are not requested`() {
            // given
            val period = calendarPeriod().start(LocalDate.of(2025, 9, 1)).end(thirdSunday).build()
            val later = itemOn(secondSunday, 2)
            val earlier = itemOn(firstSunday)
            val calendar = calendar().period(period).items(later, earlier).build()
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar))
            givenSummaries(summary)

            // when
            val result = testee.getCalendarDTOById(id, false)

            // then
            assertThat(startsOf(result.items)).containsExactly(earlier.start, later.start)
            then(entityManager).should().detach(calendar)
        }

        @Test
        fun `should add a default item for every sunday when the calendar is empty`() {
            // given
            val period = calendarPeriod().start(LocalDate.of(2025, 9, 1)).end(thirdSunday).build()
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar().period(period).build()))
            givenSummaries(summary)

            // when
            val result = testee.getCalendarDTOById(id, true)

            // then
            assertThat(startsOf(result.items)).containsExactly(firstSunday.atTime(14, 0), secondSunday.atTime(14, 0), thirdSunday.atTime(14, 0))
            assertThat(result.items).allSatisfy { assertThat(it.title).isEqualTo(defaultTitle) }
        }

        @Test
        fun `should only add defaults for sundays without an item`() {
            // given
            val period = calendarPeriod().start(LocalDate.of(2025, 9, 1)).end(thirdSunday).build()
            val existing = itemOn(secondSunday)
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar().period(period).items(existing).build()))
            givenSummaries(summary)

            // when
            val result = testee.getCalendarDTOById(id, true)

            // then
            assertThat(result.items).extracting<String> { it.title }.containsExactly(defaultTitle, "real item", defaultTitle)
        }

        @Test
        fun `should include the start and end day when they are sundays`() {
            // given
            val period = calendarPeriod().start(firstSunday).end(secondSunday).build()
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar().period(period).build()))
            givenSummaries(summary)

            // when
            val result = testee.getCalendarDTOById(id, true)

            // then
            assertThat(startsOf(result.items)).containsExactly(firstSunday.atTime(14, 0), secondSunday.atTime(14, 0))
        }

        @Test
        fun `should add no defaults when the period contains no sunday`() {
            // given
            val period = calendarPeriod().start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2025, 9, 6)).build()
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar().period(period).build()))
            givenSummaries(summary)

            // when
            val result = testee.getCalendarDTOById(id, true)

            // then
            assertThat(result.items).isEmpty()
        }

        @Test
        fun `should add a default for an empty sunday when an earlier sunday has two items`() {
            // given
            val period = calendarPeriod().start(LocalDate.of(2025, 9, 1)).end(thirdSunday).build()
            val calendar = calendar().period(period).items(itemOn(firstSunday), itemOn(firstSunday, 2)).build()
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar))
            givenSummaries(summary)

            // when
            val result = testee.getCalendarDTOById(id, true)

            // then
            assertThat(startsOf(result.items)).contains(secondSunday.atTime(14, 0))
        }
    }

    @Test
    fun `should update the intro and outro of the calendar`() {
        // given
        val calendar = calendar().intro("old intro").outro("old outro").build()
        val request = UpdateCalendarRequest("new intro", null)
        given(calendarRepository.findById(id)).willReturn(Optional.of(calendar))
        givenSummaries(summary)
        given(calendarRepository.save(calendar)).willReturn(calendar)

        // when
        testee.updateCalendar(id, request)

        // then
        assertThat(calendar.intro).isEqualTo(request.intro)
        assertThat(calendar.outro).isNull()
    }

    @Nested
    inner class CreateCalendarItem {

        @Test
        fun `should link only the calendars that exist`() {
            // given
            val calendar = calendar().build()
            val request = calendarItemRequest().calendars(id, 99).build()
            val captor = argumentCaptor<CalendarItem>()
            given(calendarRepository.findById(id)).willReturn(Optional.of(calendar))
            givenSummaries(summary)
            given(calendarRepository.findById(99)).willReturn(Optional.empty())
            given(itemRepository.save(anyValue<CalendarItem>())).willReturn(calendarItem().calendars(calendar).build())

            // when
            testee.createCalendarItem(request)

            // then
            then(itemRepository).should().save(captureValue(captor))
            assertThat(captor.value.calendars).containsExactly(calendar)
            assertThat(captor.value.title).isEqualTo(request.title)
            then(imageService).shouldHaveNoInteractions()
        }

        @Test
        fun `should move the image when the item has one`() {
            // given
            val request = calendarItemRequest().image("item.jpg").build()
            given(itemRepository.save(anyValue<CalendarItem>())).willReturn(calendarItem().image("item.jpg").build())

            // when
            testee.createCalendarItem(request)

            // then
            then(imageService).should().move("item.jpg", TEMPORARY, CALENDAR_ITEMS)
        }
    }

    @Nested
    inner class UpdateCalendarItem {

        @Test
        fun `should throw when the item does not exist`() {
            // given
            given(itemRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateCalendarItem(id, calendarItemRequest().build()) }
                .isInstanceOf(CalendarItemNotFoundException::class.java)
                .hasMessage("Calendar item not found")
        }

        @Test
        fun `should copy the fields and replace the calendars`() {
            // given
            val oldCalendar = calendar().build()
            val newCalendar = calendar().id(2).build()
            val item = calendarItem().calendars(oldCalendar).build()
            val request = calendarItemRequest().start(LocalDateTime.of(2025, 10, 5, 9, 0)).end(LocalDateTime.of(2025, 10, 5, 18, 0))
                .title("new title").content("new content").closed().calendars(2).build()
            given(itemRepository.findById(id)).willReturn(Optional.of(item))
            given(calendarRepository.findById(2)).willReturn(Optional.of(newCalendar))
            given(itemRepository.save(item)).willReturn(item)
            givenSummaries(summary)

            // when
            testee.updateCalendarItem(id, request)

            // then
            assertThat(item.start).isEqualTo(request.start)
            assertThat(item.end).isEqualTo(request.end)
            assertThat(item.title).isEqualTo(request.title)
            assertThat(item.content).isEqualTo(request.content)
            assertThat(item.closed).isTrue()
            assertThat(item.calendars).containsExactly(newCalendar)
        }

        @Test
        fun `should replace the image when it changed`() {
            // given
            val item = calendarItem().image("old.jpg").build()
            given(itemRepository.findById(id)).willReturn(Optional.of(item))
            given(itemRepository.save(item)).willReturn(item)

            // when
            testee.updateCalendarItem(id, calendarItemRequest().image("new.jpg").build())

            // then
            then(imageService).should().delete(CALENDAR_ITEMS, "old.jpg")
            then(imageService).should().move("new.jpg", TEMPORARY, CALENDAR_ITEMS)
            assertThat(item.image).isEqualTo("new.jpg")
        }

        @Test
        fun `should delete the image when it was removed`() {
            // given
            val item = calendarItem().image("old.jpg").build()
            given(itemRepository.findById(id)).willReturn(Optional.of(item))
            given(itemRepository.save(item)).willReturn(item)

            // when
            testee.updateCalendarItem(id, calendarItemRequest().image(null).build())

            // then
            then(imageService).should().delete(CALENDAR_ITEMS, "old.jpg")
            then(imageService).shouldHaveNoMoreInteractions()
            assertThat(item.image).isNull()
        }

        @Test
        fun `should keep the image when it did not change`() {
            // given
            val item = calendarItem().image("same.jpg").build()
            given(itemRepository.findById(id)).willReturn(Optional.of(item))
            given(itemRepository.save(item)).willReturn(item)

            // when
            testee.updateCalendarItem(id, calendarItemRequest().image("same.jpg").build())

            // then
            then(imageService).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class DeleteCalendarItem {

        @Test
        fun `should delete the item and its image when it has one`() {
            // given
            val item = calendarItem().image("item.jpg").build()
            given(itemRepository.findById(id)).willReturn(Optional.of(item))

            // when
            testee.deleteCalendarItem(id)

            // then
            then(imageService).should().delete(CALENDAR_ITEMS, "item.jpg")
            then(itemRepository).should().delete(item)
        }

        @Test
        fun `should only delete the item when it has no image`() {
            // given
            val item = calendarItem().build()
            given(itemRepository.findById(id)).willReturn(Optional.of(item))

            // when
            testee.deleteCalendarItem(id)

            // then
            then(imageService).shouldHaveNoInteractions()
            then(itemRepository).should().delete(item)
        }

        @Test
        fun `should throw when the item to delete does not exist`() {
            // given
            given(itemRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.deleteCalendarItem(id) }
                .isInstanceOf(CalendarItemNotFoundException::class.java)
        }
    }
}
