package be.sgl.backend.service.membership

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.UserRepository
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
class MembershipServiceIntegrationTest {

    @Autowired
    private lateinit var membershipService: MembershipService

    @Autowired
    private lateinit var membershipRepository: MembershipRepository

    @Autowired
    private lateinit var membershipPeriodRepository: MembershipPeriodRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var testBranch: Branch
    private lateinit var testPeriod: MembershipPeriod
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        membershipRepository.deleteAll()
        userRepository.deleteAll()
        branchRepository.deleteAll()
        membershipPeriodRepository.deleteAll()

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

        testPeriod = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.now().minusMonths(2)
            end = LocalDate.now().plusMonths(10)
            basePrice = 100.0
        }
        testPeriod = membershipPeriodRepository.save(testPeriod)

        testUser = User().apply {
            username = "testuser"
            firstName = "Test"
            name = "User"
            email = "testuser@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
            sex = Sex.UNKNOWN
        }
        testUser = userRepository.save(testUser)
    }

    @Test
    fun `getAllMembershipsForUser should return all user memberships`() {
        val membership1 = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership1)

        val period2 = MembershipPeriod().apply {
            name = "Period 2"
            start = LocalDate.now().plusYears(1)
            end = LocalDate.now().plusYears(2)
            basePrice = 110.0
        }
        membershipPeriodRepository.save(period2)

        val membership2 = Membership(period2, testUser, testBranch, 110.0).apply {
            paid = true
        }
        membershipRepository.save(membership2)
        membershipRepository.flush()

        val memberships = membershipService.getAllMembershipsForUser(testUser.username!!)

        assertTrue(memberships.size >= 2)
        assertTrue(memberships.any { it.period.name == "Test Period" })
        assertTrue(memberships.any { it.period.name == "Period 2" })
    }

    @Test
    fun `getCurrentMembershipForUser should return active membership`() {
        val membership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership)
        membershipRepository.flush()

        val current = membershipService.getCurrentMembershipForUser(testUser.username!!)

        assertNotNull(current)
        assertEquals(testPeriod.name, current?.period?.name)
        assertEquals(testUser.email, current?.user?.email)
    }

    @Test
    fun `getCurrentMembershipForUser should return null when no active membership`() {
        val futurePeriod = MembershipPeriod().apply {
            name = "Future Period"
            start = LocalDate.now().plusYears(1)
            end = LocalDate.now().plusYears(2)
            basePrice = 100.0
        }
        membershipPeriodRepository.save(futurePeriod)

        val membership = Membership(futurePeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership)
        membershipRepository.flush()

        val current = membershipService.getCurrentMembershipForUser(testUser.username!!)

        assertNull(current)
    }

    @Test
    fun `getCurrentMembershipsForBranch should return all current memberships for branch`() {
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

        val membership1 = Membership(testPeriod, user1, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership1)

        val membership2 = Membership(testPeriod, user2, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership2)
        membershipRepository.flush()

        val memberships = membershipService.getCurrentMembershipsForBranch(testBranch.id)

        assertTrue(memberships.size >= 2)
        assertTrue(memberships.any { it.user.email == "user1@example.com" })
        assertTrue(memberships.any { it.user.email == "user2@example.com" })
    }

    @Test
    fun `getCurrentMembershipsForBranch with null should return all current memberships`() {
        val membership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership)
        membershipRepository.flush()

        val memberships = membershipService.getCurrentMembershipsForBranch(null)

        assertTrue(memberships.size >= 1)
    }

    @Test
    fun `createMembershipForExistingUser should fail when user already has active membership`() {
        val membership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership)
        membershipRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            membershipService.createMembershipForExistingUser(testUser.username!!)
        }

        assertTrue(exception.message!!.contains("already an active membership"))
    }

    @Test
    fun `getMembershipDTOById should return membership when exists`() {
        val membership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        val saved = membershipRepository.save(membership)
        membershipRepository.flush()

        val result = membershipService.getMembershipDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals(testUser.email, result?.user?.email)
        assertEquals(testBranch.name, result?.branch?.name)
    }

    @Test
    fun `getMembershipDTOById should return null when not found`() {
        val result = membershipService.getMembershipDTOById(999)

        assertNull(result)
    }

    @Test
    fun `membership should only be active during period dates`() {
        // Create past period
        val pastPeriod = MembershipPeriod().apply {
            name = "Past Period"
            start = LocalDate.now().minusYears(2)
            end = LocalDate.now().minusYears(1)
            basePrice = 90.0
        }
        membershipPeriodRepository.save(pastPeriod)

        val pastMembership = Membership(pastPeriod, testUser, testBranch, 90.0).apply {
            paid = true
        }
        membershipRepository.save(pastMembership)

        val currentMembership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(currentMembership)
        membershipRepository.flush()

        val current = membershipService.getCurrentMembershipForUser(testUser.username!!)

        assertNotNull(current)
        assertEquals(testPeriod.name, current?.period?.name)
        assertEquals("Test Period", current?.period?.name)
    }

    @Test
    fun `getCurrentMembershipsForBranch should only return paid memberships`() {
        val paidMembership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(paidMembership)

        val unpaidUser = User().apply {
            username = "unpaiduser"
            firstName = "Unpaid"
            name = "User"
            email = "unpaid@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(unpaidUser)

        val unpaidMembership = Membership(testPeriod, unpaidUser, testBranch, 100.0).apply {
            paid = false
        }
        membershipRepository.save(unpaidMembership)
        membershipRepository.flush()

        val memberships = membershipService.getCurrentMembershipsForBranch(testBranch.id)

        // Should only contain paid membership
        assertTrue(memberships.any { it.user.email == testUser.email })
        assertFalse(memberships.any { it.user.email == unpaidUser.email })
    }

    @Test
    fun `membership for multiple branches should be tracked separately`() {
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

        val membership1 = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership1)

        val user2 = User().apply {
            username = "user2"
            firstName = "User"
            name = "Two"
            email = "user2@example.com"
            birthdate = LocalDate.of(2010, 1, 1)
        }
        userRepository.save(user2)

        val membership2 = Membership(testPeriod, user2, branch2, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership2)
        membershipRepository.flush()

        val branch1Memberships = membershipService.getCurrentMembershipsForBranch(testBranch.id)
        val branch2Memberships = membershipService.getCurrentMembershipsForBranch(branch2.id)

        assertTrue(branch1Memberships.any { it.user.email == testUser.email })
        assertFalse(branch1Memberships.any { it.user.email == user2.email })

        assertTrue(branch2Memberships.any { it.user.email == user2.email })
        assertFalse(branch2Memberships.any { it.user.email == testUser.email })
    }

    @Test
    fun `membership with reduced price should be saved correctly`() {
        val reducedUser = User().apply {
            username = "reduceduser"
            firstName = "Reduced"
            name = "User"
            email = "reduced@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
            hasReduction = true
        }
        userRepository.save(reducedUser)

        // Price reduced by factor of 3: 100.0 / 3 = 33.33
        val membership = Membership(testPeriod, reducedUser, testBranch, 33.33).apply {
            paid = true
        }
        membershipRepository.save(membership)
        membershipRepository.flush()

        val result = membershipService.getMembershipDTOById(membership.id!!)

        assertNotNull(result)
        assertEquals(33.33, result?.price)
    }

    @Test
    fun `getAllMembershipsForUser should include both paid and unpaid memberships`() {
        val paidMembership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(paidMembership)

        val period2 = MembershipPeriod().apply {
            name = "Period 2"
            start = LocalDate.now().plusYears(1)
            end = LocalDate.now().plusYears(2)
            basePrice = 110.0
        }
        membershipPeriodRepository.save(period2)

        val unpaidMembership = Membership(period2, testUser, testBranch, 110.0).apply {
            paid = false
        }
        membershipRepository.save(unpaidMembership)
        membershipRepository.flush()

        val memberships = membershipService.getAllMembershipsForUser(testUser.username!!)

        assertTrue(memberships.size >= 2)
        assertTrue(memberships.any { it.paid })
        assertTrue(memberships.any { !it.paid })
    }
}
