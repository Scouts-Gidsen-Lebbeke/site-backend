package be.sgl.backend.repository

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.calendar.Calendar
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.repository.calendar.CalendarRepository
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
class BranchRepositoryIntegrationTest {

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Autowired
    private lateinit var calendarRepository: CalendarRepository

    @BeforeEach
    fun setup() {
        branchRepository.deleteAll()
        calendarRepository.deleteAll()
    }

    @Test
    fun `getBranchesWithCalendar should return branches that have calendars`() {
        val branchWithCalendar = Branch().apply {
            name = "Branch with Calendar"
            email = "with@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Has calendar"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        val savedBranch = branchRepository.save(branchWithCalendar)

        val calendar = Calendar().apply {
            name = "Test Calendar"
            branch = savedBranch
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
        }
        calendarRepository.save(calendar)
        calendarRepository.flush()

        val branchWithoutCalendar = Branch().apply {
            name = "Branch without Calendar"
            email = "without@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "No calendar"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branchRepository.save(branchWithoutCalendar)
        branchRepository.flush()

        val branches = branchRepository.getBranchesWithCalendar()

        assertTrue(branches.any { it.name == "Branch with Calendar" })
        assertFalse(branches.any { it.name == "Branch without Calendar" })
    }

    @Test
    fun `getVisibleBranches should return non-hidden branches`() {
        val activeBranch = Branch().apply {
            name = "Active Branch"
            email = "active@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Active"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val memberBranch = Branch().apply {
            name = "Member Branch"
            email = "member@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Member"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.MEMBER
            staffTitle = "Leader"
        }

        val passiveBranch = Branch().apply {
            name = "Passive Branch"
            email = "passive@example.com"
            minimumAge = 12
            maximumAge = 14
            sex = Sex.UNKNOWN
            description = "Passive"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.PASSIVE
            staffTitle = "Leader"
        }

        val hiddenBranch = Branch().apply {
            name = "Hidden Branch"
            email = "hidden@example.com"
            minimumAge = 15
            maximumAge = 17
            sex = Sex.UNKNOWN
            description = "Hidden"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.HIDDEN
            staffTitle = "Leader"
        }

        branchRepository.save(activeBranch)
        branchRepository.save(memberBranch)
        branchRepository.save(passiveBranch)
        branchRepository.save(hiddenBranch)
        branchRepository.flush()

        val visibleBranches = branchRepository.getVisibleBranches()

        assertTrue(visibleBranches.any { it.name == "Active Branch" })
        assertTrue(visibleBranches.any { it.name == "Member Branch" })
        assertTrue(visibleBranches.any { it.name == "Passive Branch" })
        assertFalse(visibleBranches.any { it.name == "Hidden Branch" })
    }

    @Test
    fun `getPossibleBranchesForSexAndAge should match age range exactly`() {
        val branch = Branch().apply {
            name = "Exact Age Branch"
            email = "exact@example.com"
            minimumAge = 10
            maximumAge = 12
            sex = Sex.UNKNOWN
            description = "Ages 10-12"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branchRepository.save(branch)
        branchRepository.flush()

        val matchesMin = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 10)
        val matchesMax = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 12)
        val matchesMiddle = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 11)
        val tooYoung = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 9)
        val tooOld = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 13)

        assertTrue(matchesMin.any { it.name == "Exact Age Branch" })
        assertTrue(matchesMax.any { it.name == "Exact Age Branch" })
        assertTrue(matchesMiddle.any { it.name == "Exact Age Branch" })
        assertFalse(tooYoung.any { it.name == "Exact Age Branch" })
        assertFalse(tooOld.any { it.name == "Exact Age Branch" })
    }

    @Test
    fun `getPossibleBranchesForSexAndAge should respect sex restrictions`() {
        val boysBranch = Branch().apply {
            name = "Boys Only"
            email = "boys@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.MALE
            description = "For boys"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val girlsBranch = Branch().apply {
            name = "Girls Only"
            email = "girls@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.FEMALE
            description = "For girls"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val mixedBranch = Branch().apply {
            name = "Mixed"
            email = "mixed@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "For all"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        branchRepository.save(boysBranch)
        branchRepository.save(girlsBranch)
        branchRepository.save(mixedBranch)
        branchRepository.flush()

        val boysMatches = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 7)
        val girlsMatches = branchRepository.getPossibleBranchesForSexAndAge(Sex.FEMALE, 7)

        // Boys should match boys branch and mixed branch
        assertTrue(boysMatches.any { it.name == "Boys Only" })
        assertFalse(boysMatches.any { it.name == "Girls Only" })
        assertTrue(boysMatches.any { it.name == "Mixed" })

        // Girls should match girls branch and mixed branch
        assertFalse(girlsMatches.any { it.name == "Boys Only" })
        assertTrue(girlsMatches.any { it.name == "Girls Only" })
        assertTrue(girlsMatches.any { it.name == "Mixed" })
    }

    @Test
    fun `getPossibleBranchesForSexAndAge should only return active branches`() {
        val activeBranch = Branch().apply {
            name = "Active"
            email = "active@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Active"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val passiveBranch = Branch().apply {
            name = "Passive"
            email = "passive@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Passive"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.PASSIVE
            staffTitle = "Leader"
        }

        branchRepository.save(activeBranch)
        branchRepository.save(passiveBranch)
        branchRepository.flush()

        val matches = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 7)

        assertTrue(matches.any { it.name == "Active" })
        assertFalse(matches.any { it.name == "Passive" })
    }

    @Test
    fun `getPossibleBranchesForSexAndAge should return empty list when no matches`() {
        val branch = Branch().apply {
            name = "Teenage Branch"
            email = "teen@example.com"
            minimumAge = 13
            maximumAge = 17
            sex = Sex.UNKNOWN
            description = "Teens"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branchRepository.save(branch)
        branchRepository.flush()

        val matches = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 5)

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `branches should be persistable with all properties`() {
        val branch = Branch().apply {
            name = "Complete Branch"
            email = "complete@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.MALE
            description = "Complete description"
            law = "Complete law text"
            image = "complete.jpg"
            status = BranchStatus.MEMBER
            staffTitle = "Head Leader"
        }

        val saved = branchRepository.save(branch)
        branchRepository.flush()

        val retrieved = branchRepository.findById(saved.id!!).get()

        assertEquals("Complete Branch", retrieved.name)
        assertEquals("complete@example.com", retrieved.email)
        assertEquals(6, retrieved.minimumAge)
        assertEquals(8, retrieved.maximumAge)
        assertEquals(Sex.MALE, retrieved.sex)
        assertEquals("Complete description", retrieved.description)
        assertEquals("Complete law text", retrieved.law)
        assertEquals("complete.jpg", retrieved.image)
        assertEquals(BranchStatus.MEMBER, retrieved.status)
        assertEquals("Head Leader", retrieved.staffTitle)
    }
}
