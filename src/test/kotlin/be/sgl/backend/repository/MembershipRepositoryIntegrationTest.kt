package be.sgl.backend.repository

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
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
class MembershipRepositoryIntegrationTest {

    @Autowired
    private lateinit var membershipRepository: MembershipRepository

    @Autowired
    private lateinit var membershipPeriodRepository: MembershipPeriodRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    private lateinit var testPeriod: MembershipPeriod
    private lateinit var testBranch: Branch
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        membershipRepository.deleteAll()
        membershipPeriodRepository.deleteAll()
        branchRepository.deleteAll()

        testBranch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Test law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        testBranch = branchRepository.save(testBranch)

        testPeriod = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }
        testPeriod = membershipPeriodRepository.save(testPeriod)

        testUser = User().apply {
            name = "Test"
            firstName = "User"
            email = "test@example.com"
            birthdate = LocalDate.of(2010, 1, 1)
            sex = Sex.MALE
        }
    }

    @Test
    fun `countByPeriod should return correct count`() {
        // Create 3 memberships for the period
        repeat(3) { index ->
            val user = User().apply {
                name = "User$index"
                firstName = "Test"
                email = "user$index@example.com"
                birthdate = LocalDate.of(2010, 1, 1)
                sex = Sex.MALE
            }
            val membership = Membership(user, testPeriod, testBranch, 100.0)
            membershipRepository.save(membership)
        }
        membershipRepository.flush()

        val count = membershipRepository.countByPeriod(testPeriod)

        assertEquals(3, count)
    }

    @Test
    fun `countByPeriodAndBranch should return correct count for specific branch`() {
        val otherBranch = Branch().apply {
            name = "Other Branch"
            email = "other@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Other"
            law = "Other law"
            image = "other.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        otherBranch = branchRepository.save(otherBranch)

        // Create 2 memberships for test branch
        repeat(2) { index ->
            val user = User().apply {
                name = "User$index"
                firstName = "Test"
                email = "user$index@example.com"
                birthdate = LocalDate.of(2010, 1, 1)
                sex = Sex.MALE
            }
            val membership = Membership(user, testPeriod, testBranch, 100.0)
            membershipRepository.save(membership)
        }

        // Create 1 membership for other branch
        val otherUser = User().apply {
            name = "OtherUser"
            firstName = "Test"
            email = "other@example.com"
            birthdate = LocalDate.of(2005, 1, 1)
            sex = Sex.MALE
        }
        membershipRepository.save(Membership(otherUser, testPeriod, otherBranch, 100.0))
        membershipRepository.flush()

        val count = membershipRepository.countByPeriodAndBranch(testPeriod, testBranch)

        assertEquals(2, count)
    }

    @Test
    fun `getMembershipsByUser should return all memberships for user`() {
        val membership1 = Membership(testUser, testPeriod, testBranch, 100.0)
        membershipRepository.save(membership1)

        val oldPeriod = MembershipPeriod().apply {
            start = LocalDate.of(2023, 9, 1)
            end = LocalDate.of(2024, 8, 31)
            price = 90.0
        }
        membershipPeriodRepository.save(oldPeriod)

        val membership2 = Membership(testUser, oldPeriod, testBranch, 90.0)
        membershipRepository.save(membership2)
        membershipRepository.flush()

        val memberships = membershipRepository.getMembershipsByUser(testUser)

        assertEquals(2, memberships.size)
        assertTrue(memberships.any { it.period.id == testPeriod.id })
        assertTrue(memberships.any { it.period.id == oldPeriod.id })
    }

    @Test
    fun `getCurrentByUser should return only current period membership`() {
        // Set test period as current
        testPeriod.start = LocalDate.now().minusMonths(2)
        testPeriod.end = LocalDate.now().plusMonths(10)
        membershipPeriodRepository.save(testPeriod)

        val membership = Membership(testUser, testPeriod, testBranch, 100.0)
        membershipRepository.save(membership)
        membershipRepository.flush()

        val currentMembership = membershipRepository.getCurrentByUser(testUser)

        assertNotNull(currentMembership)
        assertEquals(testPeriod.id, currentMembership?.period?.id)
    }

    @Test
    fun `getCurrentByBranch should return current memberships for branch`() {
        testPeriod.start = LocalDate.now().minusMonths(2)
        testPeriod.end = LocalDate.now().plusMonths(10)
        membershipPeriodRepository.save(testPeriod)

        repeat(3) { index ->
            val user = User().apply {
                name = "User$index"
                firstName = "Test"
                email = "user$index@example.com"
                birthdate = LocalDate.of(2010, 1, 1)
                sex = Sex.MALE
            }
            val membership = Membership(user, testPeriod, testBranch, 100.0)
            membershipRepository.save(membership)
        }
        membershipRepository.flush()

        val currentMemberships = membershipRepository.getCurrentByBranch(testBranch)

        assertEquals(3, currentMemberships.size)
        assertTrue(currentMemberships.all { it.branch.id == testBranch.id })
    }

    @Test
    fun `existsByPeriodAndUser should return true when membership exists`() {
        val membership = Membership(testUser, testPeriod, testBranch, 100.0)
        membershipRepository.save(membership)
        membershipRepository.flush()

        val exists = membershipRepository.existsByPeriodAndUser(testPeriod, testUser)

        assertTrue(exists)
    }

    @Test
    fun `existsByPeriodAndUser should return false when membership does not exist`() {
        val otherUser = User().apply {
            name = "Other"
            firstName = "User"
            email = "other@example.com"
            birthdate = LocalDate.of(2010, 1, 1)
            sex = Sex.MALE
        }

        val exists = membershipRepository.existsByPeriodAndUser(testPeriod, otherUser)

        assertFalse(exists)
    }

    @Test
    fun `getCurrent should return all current memberships`() {
        testPeriod.start = LocalDate.now().minusMonths(2)
        testPeriod.end = LocalDate.now().plusMonths(10)
        membershipPeriodRepository.save(testPeriod)

        val otherBranch = Branch().apply {
            name = "Other Branch"
            email = "other@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Other"
            law = "Other law"
            image = "other.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branchRepository.save(otherBranch)

        // Create memberships for different branches
        val user1 = User().apply {
            name = "User1"
            firstName = "Test"
            email = "user1@example.com"
            birthdate = LocalDate.of(2010, 1, 1)
            sex = Sex.MALE
        }
        membershipRepository.save(Membership(user1, testPeriod, testBranch, 100.0))

        val user2 = User().apply {
            name = "User2"
            firstName = "Test"
            email = "user2@example.com"
            birthdate = LocalDate.of(2005, 1, 1)
            sex = Sex.MALE
        }
        membershipRepository.save(Membership(user2, testPeriod, otherBranch, 100.0))
        membershipRepository.flush()

        val currentMemberships = membershipRepository.getCurrent()

        assertEquals(2, currentMemberships.size)
    }
}
