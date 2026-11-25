package be.sgl.backend.calendar.web

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.calendar.domain.CalendarItemRepository
import be.sgl.backend.calendar.domain.CalendarPeriodRepository
import be.sgl.backend.calendar.domain.CalendarPeriodTestMother.calendarPeriod
import be.sgl.backend.calendar.domain.CalendarRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@IntegrationTest
class CalendarPeriodControllerTest {

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

    private fun postPeriod(start: LocalDate, end: LocalDate) = mockMvc.perform(post("/calendars/periods")
        .contentType(APPLICATION_JSON)
        .content("""{"name":"Najaar","start":"$start","end":"$end"}""")
        .with(jwt().jwt { it.subject(ADMIN) }))

    @Test
    fun `should create calendars only for active branches when staff creates a period`() {
        // given
        users.admin(ADMIN)
        val active = branchRepository.save(branch().id(null).name("Kapoenen").active().build())
        branchRepository.save(branch().id(null).name("Leiding").passive().build())
        branchRepository.save(branch().id(null).name("Verborgen").hidden().build())

        // when
        val result = postPeriod(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31))

        // then
        result.andExpect(status().isCreated)
        assertThat(calendarRepository.findAll()).extracting<Any> { it.branchId }.containsExactly(active.id)
        assertThat(periodRepository.count()).isEqualTo(1)
    }

    @Test
    fun `should return 400 when the new period overlaps an existing one`() {
        // given
        users.admin(ADMIN)
        periodRepository.save(calendarPeriod().id(null).start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2025, 12, 31)).build())

        // when
        val result = postPeriod(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 2, 28))

        // then
        result.andExpect(status().isBadRequest)
        assertThat(periodRepository.count()).isEqualTo(1)
    }

    companion object {
        private const val ADMIN = "admin"
    }
}
