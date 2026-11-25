package be.sgl.backend.calendar.web

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.calendar.domain.Calendar
import be.sgl.backend.calendar.domain.CalendarItemRepository
import be.sgl.backend.calendar.domain.CalendarItemTestMother.calendarItem
import be.sgl.backend.calendar.domain.CalendarPeriodRepository
import be.sgl.backend.calendar.domain.CalendarPeriodTestMother.calendarPeriod
import be.sgl.backend.calendar.domain.CalendarRepository
import be.sgl.backend.calendar.domain.CalendarTestMother.calendar
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.util.IntegrationTest
import be.sgl.backend.shared.util.PersistedUsers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@IntegrationTest
class CalendarControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var calendarRepository: CalendarRepository
    @Autowired
    private lateinit var periodRepository: CalendarPeriodRepository
    @Autowired
    private lateinit var itemRepository: CalendarItemRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    private val users by lazy { PersistedUsers(userRepository, roleRepository, userRoleRepository) }

    @AfterEach
    fun cleanUp() {
        itemRepository.deleteAll()
        calendarRepository.deleteAll()
        periodRepository.deleteAll()
        branchRepository.deleteAll()
        users.deleteAll()
    }

    private fun persistedCalendar(periodStart: LocalDate, periodEnd: LocalDate): Calendar {
        val branch = branchRepository.save(branch().id(null).name(BRANCH_NAME).image(BRANCH_IMAGE).build())
        val period = periodRepository.save(calendarPeriod().id(null).start(periodStart).end(periodEnd).build())
        return calendarRepository.save(calendar().id(null).period(period).branchId(branch.id!!).build())
    }

    @Test
    fun `should return branch id name and image when getting current calendars`() {
        // given
        val today = LocalDate.now()
        val calendar = persistedCalendar(today.minusDays(7), today.plusDays(7))

        // when
        val result = mockMvc.perform(get("/calendars/current"))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(calendar.id))
            .andExpect(jsonPath("$[0].branch.id").value(calendar.branchId))
            .andExpect(jsonPath("$[0].branch.name").value(BRANCH_NAME))
            .andExpect(jsonPath("$[0].branch.image").value(BRANCH_IMAGE))
            .andExpect(jsonPath("$[0].branch.length()").value(3))
    }

    @Test
    fun `should fill empty sundays without persisting them when requesting defaults`() {
        // given
        val firstMonday = LocalDate.of(2025, 9, 1)
        val calendar = persistedCalendar(firstMonday, firstMonday.plusDays(20))
        val secondSunday = firstMonday.plusDays(13)
        itemRepository.save(calendarItem().id(null).start(secondSunday.atTime(10, 0)).end(secondSunday.atTime(12, 0)).calendars(calendar).build())

        // when
        val withDefaults = mockMvc.perform(get("/calendars/${calendar.id}").param("withDefaults", "true"))
        val plain = mockMvc.perform(get("/calendars/${calendar.id}"))

        // then
        withDefaults.andExpect(status().isOk).andExpect(jsonPath("$.items.length()").value(3))
        plain.andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].title").value("calendarItemTitle"))
        assertThat(itemRepository.count()).isEqualTo(1)
    }

    @Test
    fun `should return 404 when calendar id is unknown`() {
        // given
        val unknownId = 999999

        // when
        val result = mockMvc.perform(get("/calendars/$unknownId"))

        // then
        result.andExpect(status().isNotFound)
    }

    @Test
    fun `should return 403 when a non-staff user updates a calendar`() {
        // given
        users.withoutRoles(MEMBER)
        val calendar = persistedCalendar(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31))

        // when
        val result = mockMvc.perform(put("/calendars/${calendar.id}")
            .contentType(APPLICATION_JSON)
            .content("""{"intro":"new intro","outro":"new outro"}""")
            .with(jwt().jwt { it.subject(MEMBER) }))

        // then
        result.andExpect(status().isForbidden)
        assertThat(calendarRepository.findById(calendar.id!!).get().intro).isNull()
    }

    companion object {
        private const val MEMBER = "member"
        private const val BRANCH_NAME = "Kapoenen"
        private const val BRANCH_IMAGE = "kapoenen.png"
    }
}
