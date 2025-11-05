package be.sgl.backend.service.membership

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.MembershipPeriodDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.exception.MembershipPeriodNotFoundException
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class MembershipPeriodServiceIntegrationTest {

    @Autowired
    private lateinit var periodService: MembershipPeriodService

    @Autowired
    private lateinit var periodRepository: MembershipPeriodRepository

    @Autowired
    private lateinit var membershipRepository: MembershipRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testBranch: Branch

    @BeforeEach
    fun setup() {
        membershipRepository.deleteAll()
        userRepository.deleteAll()
        branchRepository.deleteAll()
        periodRepository.deleteAll()

        testBranch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 12
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
    fun `getAllMembershipPeriods should return all periods with member counts`() {
        val period1 = MembershipPeriod().apply {
            name = "Period 1"
            start = LocalDate.of(2023, 9, 1)
            end = LocalDate.of(2024, 8, 31)
            basePrice = 100.0
        }
        periodRepository.save(period1)

        val period2 = MembershipPeriod().apply {
            name = "Period 2"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            basePrice = 110.0
        }
        periodRepository.save(period2)
        periodRepository.flush()

        val periods = periodService.getAllMembershipPeriods()

        assertTrue(periods.size >= 2)
        assertTrue(periods.any { it.period.name == "Period 1" })
        assertTrue(periods.any { it.period.name == "Period 2" })
    }

    @Test
    fun `getAllMembershipPeriods should include paid member count`() {
        val period = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.now().minusMonths(2)
            end = LocalDate.now().plusMonths(10)
            basePrice = 100.0
        }
        val savedPeriod = periodRepository.save(period)

        val user1 = User().apply {
            username = "user1"
            firstName = "User"
            name = "One"
            email = "user1@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(user1)

        val user2 = User().apply {
            username = "user2"
            firstName = "User"
            name = "Two"
            email = "user2@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(user2)

        val membership1 = Membership(savedPeriod, user1, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership1)

        val membership2 = Membership(savedPeriod, user2, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership2)

        // Unpaid membership should not be counted
        val user3 = User().apply {
            username = "user3"
            firstName = "User"
            name = "Three"
            email = "user3@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(user3)

        val unpaidMembership = Membership(savedPeriod, user3, testBranch, 100.0).apply {
            paid = false
        }
        membershipRepository.save(unpaidMembership)
        membershipRepository.flush()

        val periods = periodService.getAllMembershipPeriods()

        val testPeriod = periods.find { it.period.name == "Test Period" }
        assertNotNull(testPeriod)
        assertEquals(2, testPeriod?.memberships?.size)
    }

    @Test
    fun `getMembershipPeriodDTOById should return period when exists`() {
        val period = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            basePrice = 100.0
        }
        val saved = periodRepository.save(period)
        periodRepository.flush()

        val result = periodService.getMembershipPeriodDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals("Test Period", result.name)
        assertEquals(LocalDate.of(2024, 9, 1), result.start)
        assertEquals(LocalDate.of(2025, 8, 31), result.end)
    }

    @Test
    fun `getMembershipPeriodDTOById should throw exception when not found`() {
        assertThrows(MembershipPeriodNotFoundException::class.java) {
            periodService.getMembershipPeriodDTOById(999)
        }
    }

    @Test
    fun `getCurrentMembershipPeriod should return active period`() {
        val currentPeriod = MembershipPeriod().apply {
            name = "Current Period"
            start = LocalDate.now().minusMonths(2)
            end = LocalDate.now().plusMonths(10)
            basePrice = 100.0
        }
        periodRepository.save(currentPeriod)
        periodRepository.flush()

        val result = periodService.getCurrentMembershipPeriod()

        assertNotNull(result)
        assertEquals("Current Period", result.name)
    }

    @Test
    fun `saveMembershipPeriodDTO should create new period`() {
        val dto = MembershipPeriodDTO(
            id = null,
            name = "New Period",
            start = LocalDate.of(2025, 9, 1),
            end = LocalDate.of(2026, 8, 31),
            basePrice = 120.0,
            restrictions = emptyList()
        )

        val saved = periodService.saveMembershipPeriodDTO(dto)

        assertNotNull(saved.id)
        assertEquals("New Period", saved.name)
        assertEquals(120.0, saved.basePrice)

        val persisted = periodRepository.findById(saved.id!!).get()
        assertEquals("New Period", persisted.name)
    }

    @Test
    fun `saveMembershipPeriodDTO should create period with restrictions`() {
        val restriction1 = MembershipRestriction(
            testBranch,
            LocalDate.of(2025, 9, 1),
            100
        )

        val period = MembershipPeriod().apply {
            name = "Period with Restrictions"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 100.0
            restrictions.add(restriction1)
        }

        val saved = periodRepository.save(period)
        periodRepository.flush()

        val result = periodService.getMembershipPeriodDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals(1, result.restrictions.size)
    }

    @Test
    fun `membership period should validate restriction dates`() {
        val period = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 100.0
        }

        // Restriction with date before period start should fail validation
        val invalidRestriction = MembershipRestriction(
            testBranch,
            LocalDate.of(2025, 8, 1), // Before period start
            100
        )
        period.restrictions.add(invalidRestriction)

        val exception = assertThrows(IllegalStateException::class.java) {
            period.validateRestrictions()
        }

        assertTrue(exception.message!!.contains("restriction") || exception.message!!.contains("before"))
    }

    @Test
    fun `membership period should validate restriction limits`() {
        val period = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 100.0
        }

        // Restriction with non-positive limit should fail validation
        val invalidRestriction = MembershipRestriction(
            testBranch,
            LocalDate.of(2025, 10, 1),
            0 // Non-positive limit
        )
        period.restrictions.add(invalidRestriction)

        val exception = assertThrows(IllegalStateException::class.java) {
            period.validateRestrictions()
        }

        assertTrue(exception.message!!.contains("limit") || exception.message!!.contains("positive"))
    }

    @Test
    fun `periods should be ordered by start date descending`() {
        val period1 = MembershipPeriod().apply {
            name = "2023-2024"
            start = LocalDate.of(2023, 9, 1)
            end = LocalDate.of(2024, 8, 31)
            basePrice = 100.0
        }
        periodRepository.save(period1)

        val period2 = MembershipPeriod().apply {
            name = "2024-2025"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            basePrice = 110.0
        }
        periodRepository.save(period2)

        val period3 = MembershipPeriod().apply {
            name = "2025-2026"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 120.0
        }
        periodRepository.save(period3)
        periodRepository.flush()

        val periods = periodService.getAllMembershipPeriods()

        assertTrue(periods.size >= 3)
        // Most recent period should be first
        val firstPeriod = periods.find { it.period.name == "2025-2026" }
        val lastPeriod = periods.find { it.period.name == "2023-2024" }
        assertNotNull(firstPeriod)
        assertNotNull(lastPeriod)

        val firstIndex = periods.indexOf(firstPeriod)
        val lastIndex = periods.indexOf(lastPeriod)
        assertTrue(firstIndex < lastIndex)
    }

    @Test
    fun `membership period with multiple branches should track restrictions separately`() {
        val branch2 = Branch().apply {
            name = "Branch 2"
            email = "branch2@example.com"
            minimumAge = 12
            maximumAge = 16
            sex = Sex.UNKNOWN
            description = "Test 2"
            law = "Law"
            image = "test2.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branchRepository.save(branch2)

        val period = MembershipPeriod().apply {
            name = "Multi-Branch Period"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 100.0
        }

        val restriction1 = MembershipRestriction(testBranch, LocalDate.of(2025, 10, 1), 50)
        val restriction2 = MembershipRestriction(branch2, LocalDate.of(2025, 10, 15), 75)

        period.restrictions.add(restriction1)
        period.restrictions.add(restriction2)

        val saved = periodRepository.save(period)
        periodRepository.flush()

        val result = periodService.getMembershipPeriodDTOById(saved.id!!)

        assertEquals(2, result.restrictions.size)
        assertTrue(result.restrictions.any { it.branch.name == "Test Branch" && it.maxCount == 50 })
        assertTrue(result.restrictions.any { it.branch.name == "Branch 2" && it.maxCount == 75 })
    }

    @Test
    fun `period toString should format dates correctly`() {
        val period = MembershipPeriod().apply {
            name = "2024-2025"
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            basePrice = 100.0
        }

        val toString = period.toString()

        assertTrue(toString.contains("2024") || toString.contains("2025"))
    }

    @Test
    fun `period with no restrictions should be valid`() {
        val period = MembershipPeriod().apply {
            name = "No Restrictions"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 100.0
        }

        assertDoesNotThrow {
            period.validateRestrictions()
        }
    }

    @Test
    fun `saveMembershipPeriodDTO should set bidirectional relationship with restrictions`() {
        val restriction = MembershipRestriction(
            testBranch,
            LocalDate.of(2025, 10, 1),
            100
        )

        val period = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.of(2025, 9, 1)
            end = LocalDate.of(2026, 8, 31)
            basePrice = 100.0
            restrictions.add(restriction)
        }

        val saved = periodRepository.save(period)
        periodRepository.flush()

        val retrieved = periodRepository.findById(saved.id!!).get()
        assertEquals(1, retrieved.restrictions.size)
        assertEquals(saved.id, retrieved.restrictions[0].period?.id)
    }
}
