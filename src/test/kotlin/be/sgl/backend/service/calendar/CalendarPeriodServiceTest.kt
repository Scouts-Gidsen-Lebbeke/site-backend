package be.sgl.backend.service.calendar

import be.sgl.backend.dto.calendar.CalendarRequestTestMother.calendarPeriodRequest
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItemTestMother.calendarItem
import be.sgl.backend.entity.calendar.CalendarPeriod
import be.sgl.backend.entity.calendar.CalendarPeriodTestMother.calendarPeriod
import be.sgl.backend.entity.calendar.CalendarTestMother.calendar
import be.sgl.backend.shared.exception.CalendarPeriodNotFoundException
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.calendar.CalendarItemRepository
import be.sgl.backend.repository.calendar.CalendarPeriodRepository
import be.sgl.backend.repository.calendar.CalendarRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CalendarPeriodServiceTest {

    @Mock
    lateinit var periodRepository: CalendarPeriodRepository
    @Mock
    lateinit var calendarRepository: CalendarRepository
    @Mock
    lateinit var itemRepository: CalendarItemRepository
    @Mock
    lateinit var branchRepository: BranchRepository
    @InjectMocks
    lateinit var testee: CalendarPeriodService

    private val id = 1

    @Nested
    inner class CreateCalendarPeriod {

        @Test
        fun `should throw when the period overlaps with existing periods`() {
            // given
            val request = calendarPeriodRequest().build()
            given(periodRepository.getOverlappingPeriods(request.start!!, request.end!!)).willReturn(listOf(
                calendarPeriod().name("Najaar").build(),
                calendarPeriod().id(2).name("Winter").build()
            ))

            // when / then
            assertThatThrownBy { testee.createCalendarPeriod(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Calendar period overlaps with existing periods: Najaar, Winter")
            then(periodRepository).should(never()).save(anyValue<CalendarPeriod>())
        }

        @Test
        fun `should create a calendar for every branch with a calendar`() {
            // given
            val request = calendarPeriodRequest().build()
            val saved = calendarPeriod().build()
            val firstBranch = branch().build()
            val secondBranch = branch().id(2).build()
            val captor = argumentCaptor<Calendar>()
            given(periodRepository.getOverlappingPeriods(request.start!!, request.end!!)).willReturn(emptyList())
            given(periodRepository.save(anyValue<CalendarPeriod>())).willReturn(saved)
            given(branchRepository.getBranchesWithCalendar()).willReturn(listOf(firstBranch, secondBranch))

            // when
            testee.createCalendarPeriod(request)

            // then
            then(calendarRepository).should(times(2)).save(captureValue(captor))
            assertThat(captor.allValues).extracting<Any> { it.branch }.containsExactly(firstBranch, secondBranch)
            assertThat(captor.allValues).allSatisfy { assertThat(it.period).isSameAs(saved) }
        }

        @Test
        fun `should save the period with the requested fields`() {
            // given
            val request = calendarPeriodRequest().name("Voorjaar").build()
            val captor = argumentCaptor<CalendarPeriod>()
            given(periodRepository.getOverlappingPeriods(request.start!!, request.end!!)).willReturn(emptyList())
            given(periodRepository.save(anyValue<CalendarPeriod>())).willReturn(calendarPeriod().build())
            given(branchRepository.getBranchesWithCalendar()).willReturn(emptyList())

            // when
            testee.createCalendarPeriod(request)

            // then
            then(periodRepository).should().save(captureValue(captor))
            assertThat(captor.value.name).isEqualTo(request.name)
            assertThat(captor.value.start).isEqualTo(request.start)
            assertThat(captor.value.end).isEqualTo(request.end)
            then(calendarRepository).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class UpdateCalendarPeriod {

        @Test
        fun `should throw when the period does not exist`() {
            // given
            given(periodRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateCalendarPeriod(id, calendarPeriodRequest().build()) }
                .isInstanceOf(CalendarPeriodNotFoundException::class.java)
                .hasMessage("Calendar period not found")
        }

        @Test
        fun `should ignore the period itself when checking for overlaps`() {
            // given
            val period = calendarPeriod().id(id).build()
            val request = calendarPeriodRequest().name("new name").start(LocalDate.of(2025, 9, 2)).end(LocalDate.of(2025, 12, 30)).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.getOverlappingPeriods(request.start!!, request.end!!)).willReturn(listOf(period))
            given(periodRepository.save(period)).willReturn(period)

            // when
            testee.updateCalendarPeriod(id, request)

            // then
            assertThat(period.name).isEqualTo(request.name)
            assertThat(period.start).isEqualTo(request.start)
            assertThat(period.end).isEqualTo(request.end)
        }

        @Test
        fun `should throw when the updated period overlaps with another period`() {
            // given
            val period = calendarPeriod().id(id).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.getOverlappingPeriods(period.start, period.end))
                .willReturn(listOf(period, calendarPeriod().id(2).name("other").build()))

            // when / then
            assertThatThrownBy { testee.updateCalendarPeriod(id, calendarPeriodRequest().build()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Calendar period overlaps with existing periods: other")
        }

        @Test
        fun `should keep the current values when the request fields are null`() {
            // given
            val period = calendarPeriod().id(id).name("name").build()
            val originalStart = period.start
            val originalEnd = period.end
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.getOverlappingPeriods(originalStart, originalEnd)).willReturn(emptyList())
            given(periodRepository.save(period)).willReturn(period)

            // when
            testee.updateCalendarPeriod(id, calendarPeriodRequest().name(null).start(null).end(null).build())

            // then
            assertThat(period.name).isEqualTo("name")
            assertThat(period.start).isEqualTo(originalStart)
            assertThat(period.end).isEqualTo(originalEnd)
        }
    }

    @Nested
    inner class DeleteCalendarPeriod {

        @Test
        fun `should throw when the period does not exist`() {
            // given
            given(periodRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.deleteCalendarPeriod(id) }
                .isInstanceOf(CalendarPeriodNotFoundException::class.java)
        }

        @Test
        fun `should delete the items only in this calendar and unlink the shared items`() {
            // given
            val period = calendarPeriod().build()
            val calendar = calendar().period(period).build()
            val otherCalendar = calendar().id(2).build()
            val ownItem = calendarItem().calendars(calendar).build()
            val sharedItem = calendarItem().id(2).calendars(calendar, otherCalendar).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(calendarRepository.getCalendarsByPeriod(period)).willReturn(listOf(calendar))

            // when
            testee.deleteCalendarPeriod(id)

            // then
            then(itemRepository).should().delete(ownItem)
            then(itemRepository).should().save(sharedItem)
            assertThat(sharedItem.calendars).containsExactly(otherCalendar)
            then(calendarRepository).should().delete(calendar)
            then(periodRepository).should().delete(period)
        }
    }
}
