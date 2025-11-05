package be.sgl.backend.service

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.CalendarPeriodDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.calendar.CalendarItem
import be.sgl.backend.entity.calendar.CalendarPeriod
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.calendar.CalendarItemRepository
import be.sgl.backend.repository.calendar.CalendarPeriodRepository
import be.sgl.backend.repository.calendar.CalendarRepository
import be.sgl.backend.service.exception.CalendarPeriodNotFoundException
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.DayOfWeek
import java.time.LocalDate

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class CalendarServiceIntegrationTest {

    @Autowired
    private lateinit var calendarService: CalendarService

    @Autowired
    private lateinit var periodRepository: CalendarPeriodRepository

    @Autowired
    private lateinit var calendarRepository: CalendarRepository

    @Autowired
    private lateinit var itemRepository: CalendarItemRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    private lateinit var testBranch: Branch

    @BeforeEach
    fun setup() {
        itemRepository.deleteAll()
        calendarRepository.deleteAll()
        periodRepository.deleteAll()
        branchRepository.deleteAll()

        testBranch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        testBranch = branchRepository.save(testBranch)
    }

    @Test
    fun `saveCalendarPeriodDTO should create period and calendars for all branches with calendars`() {
        // Create another branch
        val branch2 = Branch().apply {
            name = "Branch 2"
            email = "branch2@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Test 2"
            law = "Law"
            image = "test2.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branchRepository.save(branch2)

        // Create calendar for first branch to mark it as "has calendar"
        val existingPeriod = CalendarPeriod().apply {
            name = "Existing Period"
            start = LocalDate.of(2023, 9, 1)
            end = LocalDate.of(2024, 8, 31)
        }
        val savedPeriod = periodRepository.save(existingPeriod)
        calendarRepository.save(Calendar(savedPeriod, testBranch))
        calendarRepository.flush()

        val dto = CalendarPeriodDTO(
            id = null,
            name = "2024-2025",
            start = LocalDate.of(2024, 9, 1),
            end = LocalDate.of(2025, 8, 31)
        )

        val saved = calendarService.saveCalendarPeriodDTO(dto)

        assertNotNull(saved.id)
        assertEquals("2024-2025", saved.name)

        // Should create calendar for branch with existing calendar
        val calendars = calendarRepository.getCalendarsByPeriod(periodRepository.findById(saved.id!!).get())
        assertEquals(1, calendars.size)
        assertEquals(testBranch.id, calendars[0].branch.id)
    }

    @Test
    fun `saveCalendarPeriodDTO should reject overlapping periods`() {
        val existingPeriod = CalendarPeriod().apply {
            name = "Existing Period"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        periodRepository.save(existingPeriod)

        val overlappingDto = CalendarPeriodDTO(
            id = null,
            name = "Overlapping Period",
            start = LocalDate.of(2025, 1, 1),
            end = LocalDate.of(2025, 12, 31)
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            calendarService.saveCalendarPeriodDTO(overlappingDto)
        }

        assertTrue(exception.message!!.contains("overlaps with existing periods"))
    }

    @Test
    fun `saveCalendarPeriodDTO should allow adjacent periods`() {
        val existingPeriod = CalendarPeriod().apply {
            name = "Existing Period"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        periodRepository.save(existingPeriod)

        val adjacentDto = CalendarPeriodDTO(
            id = null,
            name = "Adjacent Period",
            start = LocalDate.of(2025, 9, 1),
            end = LocalDate.of(2026, 8, 31)
        )

        val saved = calendarService.saveCalendarPeriodDTO(adjacentDto)

        assertNotNull(saved.id)
        assertEquals("Adjacent Period", saved.name)
    }

    @Test
    fun `deleteCalendarPeriod should remove all associated calendars and items`() {
        val period = CalendarPeriod().apply {
            name = "To Delete"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        val savedPeriod = periodRepository.save(period)

        val calendar = Calendar(savedPeriod, testBranch)
        val savedCalendar = calendarRepository.save(calendar)

        val item = CalendarItem().apply {
            title = "Test Item"
            content = "Content"
            start = LocalDate.of(2024, 10, 6).atTime(14, 0)
            end = LocalDate.of(2024, 10, 6).atTime(16, 0)
            calendars = mutableListOf(savedCalendar)
        }
        itemRepository.save(item)
        itemRepository.flush()

        calendarService.deleteCalendarPeriod(savedPeriod.id!!)

        assertFalse(periodRepository.existsById(savedPeriod.id!!))
        assertFalse(calendarRepository.existsById(savedCalendar.id!!))
        // Item should be deleted because it's not linked to any other calendar
        assertFalse(itemRepository.existsById(item.id!!))
    }

    @Test
    fun `deleteCalendarPeriod should keep items linked to other calendars`() {
        val period1 = CalendarPeriod().apply {
            name = "Period 1"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        val savedPeriod1 = periodRepository.save(period1)

        val period2 = CalendarPeriod().apply {
            name = "Period 2"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
        }
        val savedPeriod2 = periodRepository.save(period2)

        val calendar1 = calendarRepository.save(Calendar(savedPeriod1, testBranch))
        val calendar2 = calendarRepository.save(Calendar(savedPeriod2, testBranch))

        val sharedItem = CalendarItem().apply {
            title = "Shared Item"
            content = "Content"
            start = LocalDate.of(2024, 10, 6).atTime(14, 0)
            end = LocalDate.of(2024, 10, 6).atTime(16, 0)
            calendars = mutableListOf(calendar1, calendar2)
        }
        val savedItem = itemRepository.save(sharedItem)
        itemRepository.flush()

        calendarService.deleteCalendarPeriod(savedPeriod1.id!!)

        // Item should still exist, linked to calendar2
        assertTrue(itemRepository.existsById(savedItem.id!!))
        val remainingItem = itemRepository.findById(savedItem.id!!).get()
        assertEquals(1, remainingItem.calendars.size)
        assertEquals(calendar2.id, remainingItem.calendars[0].id)
    }

    @Test
    fun `getCurrentCalendars should return calendars in current period`() {
        val currentPeriod = CalendarPeriod().apply {
            name = "Current"
            start = LocalDate.now().minusMonths(2)
            end = LocalDate.now().plusMonths(10)
        }
        val savedPeriod = periodRepository.save(currentPeriod)
        calendarRepository.save(Calendar(savedPeriod, testBranch))
        calendarRepository.flush()

        val calendars = calendarService.getCurrentCalendars()

        assertTrue(calendars.size >= 1)
        assertTrue(calendars.any { it.period.name == "Current" })
    }

    @Test
    fun `getCalendarDTOById with withDefaults should generate default Sunday items`() {
        val period = CalendarPeriod().apply {
            name = "Test Period"
            start = LocalDate.of(2024, 10, 1) // Tuesday
            end = LocalDate.of(2024, 10, 31)
        }
        val savedPeriod = periodRepository.save(period)
        val calendar = calendarRepository.save(Calendar(savedPeriod, testBranch))
        calendarRepository.flush()

        val calendarDTO = calendarService.getCalendarDTOById(calendar.id!!, withDefaults = true)

        // October 2024 has Sundays on: 6, 13, 20, 27
        // Should generate default items for these dates
        assertTrue(calendarDTO.items.size >= 4)
        assertTrue(calendarDTO.items.any { it.start.toLocalDate().dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test
    fun `getCalendarDTOById without withDefaults should not generate items`() {
        val period = CalendarPeriod().apply {
            name = "Test Period"
            start = LocalDate.of(2024, 10, 1)
            end = LocalDate.of(2024, 10, 31)
        }
        val savedPeriod = periodRepository.save(period)
        val calendar = calendarRepository.save(Calendar(savedPeriod, testBranch))
        calendarRepository.flush()

        val calendarDTO = calendarService.getCalendarDTOById(calendar.id!!, withDefaults = false)

        assertEquals(0, calendarDTO.items.size)
    }

    @Test
    fun `getAllCalendarPeriods should return all periods`() {
        val period1 = CalendarPeriod().apply {
            name = "Period 1"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        val period2 = CalendarPeriod().apply {
            name = "Period 2"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
        }
        periodRepository.save(period1)
        periodRepository.save(period2)
        periodRepository.flush()

        val periods = calendarService.getAllCalendarPeriods()

        assertTrue(periods.size >= 2)
        assertTrue(periods.any { it.name == "Period 1" })
        assertTrue(periods.any { it.name == "Period 2" })
    }

    @Test
    fun `mergeCalendarPeriodDTOChanges should update period dates`() {
        val period = CalendarPeriod().apply {
            name = "Original"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        val saved = periodRepository.save(period)
        periodRepository.flush()

        val dto = CalendarPeriodDTO(
            id = saved.id,
            name = "Updated",
            start = LocalDate.of(2024, 10, 1),
            end = LocalDate.of(2025, 9, 30)
        )

        val updated = calendarService.mergeCalendarPeriodDTOChanges(saved.id!!, dto)

        assertEquals("Updated", updated.name)
        assertEquals(LocalDate.of(2024, 10, 1), updated.start)
        assertEquals(LocalDate.of(2025, 9, 30), updated.end)
    }

    @Test
    fun `mergeCalendarPeriodDTOChanges should reject overlaps with other periods`() {
        val period1 = CalendarPeriod().apply {
            name = "Period 1"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        val saved1 = periodRepository.save(period1)

        val period2 = CalendarPeriod().apply {
            name = "Period 2"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
        }
        val saved2 = periodRepository.save(period2)
        periodRepository.flush()

        val dto = CalendarPeriodDTO(
            id = saved2.id,
            name = "Period 2",
            start = LocalDate.of(2025, 1, 1), // Overlaps with period1
            end = LocalDate.of(2026, 8, 31)
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            calendarService.mergeCalendarPeriodDTOChanges(saved2.id!!, dto)
        }

        assertTrue(exception.message!!.contains("overlaps"))
    }

    @Test
    fun `getCalendarPeriodDTOById should throw exception when not found`() {
        assertThrows(CalendarPeriodNotFoundException::class.java) {
            calendarService.getCalendarPeriodDTOById(999)
        }
    }
}
