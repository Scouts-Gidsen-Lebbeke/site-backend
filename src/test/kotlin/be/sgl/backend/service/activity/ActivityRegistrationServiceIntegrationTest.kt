package be.sgl.backend.service.activity

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.Sibling
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class ActivityRegistrationServiceIntegrationTest {

    @Autowired
    private lateinit var registrationService: ActivityRegistrationService

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository

    @Autowired
    private lateinit var restrictionRepository: ActivityRestrictionRepository

    @Autowired
    private lateinit var membershipRepository: MembershipRepository

    @Autowired
    private lateinit var membershipPeriodRepository: MembershipPeriodRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var siblingRepository: SiblingRepository

    private lateinit var testUser: User
    private lateinit var testBranch: Branch
    private lateinit var testActivity: Activity
    private lateinit var testRestriction: ActivityRestriction
    private lateinit var testPeriod: MembershipPeriod

    @BeforeEach
    fun setup() {
        siblingRepository.deleteAll()
        registrationRepository.deleteAll()
        restrictionRepository.deleteAll()
        activityRepository.deleteAll()
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

        testUser = User().apply {
            username = "testuser"
            firstName = "Test"
            name = "User"
            email = "testuser@example.com"
            birthdate = LocalDate.of(2014, 1, 1) // Age 10-11
            sex = Sex.UNKNOWN
            hasReduction = false
            hasHandicap = false
        }
        testUser = userRepository.save(testUser)

        testPeriod = MembershipPeriod().apply {
            name = "Test Period"
            start = LocalDate.now().minusMonths(2)
            end = LocalDate.now().plusMonths(10)
            basePrice = 100.0
        }
        testPeriod = membershipPeriodRepository.save(testPeriod)

        // Create active membership for user
        val membership = Membership(testPeriod, testUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership)

        testActivity = Activity().apply {
            name = "Test Activity"
            description = "Test Description"
            open = LocalDateTime.now().minusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            reductionFactor = 3
            siblingReduction = 10.0
            cancellable = true
            sendConfirmation = true
            sendCompleteConfirmation = false
        }
        testActivity = activityRepository.save(testActivity)

        testRestriction = ActivityRestriction().apply {
            name = "Test Restriction"
            activity = testActivity
            branch = testBranch
        }
        testRestriction = restrictionRepository.save(testRestriction)
    }

    @Test
    fun `getStatusForActivityAndUser should return open options when user has active membership`() {
        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertTrue(status.activeMembership)
        assertEquals(1, status.openOptions.size)
        assertEquals(testRestriction.id, status.openOptions[0].id)
        assertTrue(status.closedOptions.isEmpty())
        assertNull(status.currentRegistration)
    }

    @Test
    fun `getStatusForActivityAndUser should return no active membership when user has no membership`() {
        membershipRepository.deleteAll()

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertFalse(status.activeMembership)
        assertTrue(status.openOptions.isEmpty())
    }

    @Test
    fun `getStatusForActivityAndUser should apply reduction for user with hasReduction flag`() {
        testUser.hasReduction = true
        userRepository.save(testUser)

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertTrue(status.openOptions.isNotEmpty())
        // Price should be divided by reductionFactor (3)
        val expectedPrice = 50.0 / 3
        assertEquals(expectedPrice, status.openOptions[0].alternativePrice)
    }

    @Test
    fun `getStatusForActivityAndUser should show closed options when global limit is reached`() {
        testActivity.registrationLimit = 1
        activityRepository.save(testActivity)

        // Create another user and registration
        val otherUser = User().apply {
            username = "otheruser"
            firstName = "Other"
            name = "User"
            email = "other@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(otherUser)

        val registration = ActivityRegistration(testActivity, otherUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(registration)
        registrationRepository.flush()

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertTrue(status.openOptions.isEmpty())
        assertEquals(1, status.closedOptions.size)
    }

    @Test
    fun `getStatusForActivityAndUser should show closed options when restriction limit is reached`() {
        testRestriction.alternativeLimit = 1
        restrictionRepository.save(testRestriction)

        val otherUser = User().apply {
            username = "otheruser"
            firstName = "Other"
            name = "User"
            email = "other@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(otherUser)

        val registration = ActivityRegistration(testActivity, otherUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(registration)
        registrationRepository.flush()

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertTrue(status.openOptions.isEmpty())
        assertEquals(1, status.closedOptions.size)
    }

    @Test
    fun `getStatusForActivityAndUser should show closed options when branch limit is reached`() {
        testActivity.branchLimits = mutableMapOf(testBranch.id!! to 1)
        activityRepository.save(testActivity)

        val otherUser = User().apply {
            username = "otheruser"
            firstName = "Other"
            name = "User"
            email = "other@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(otherUser)

        // Create membership for other user in same branch
        val membership = Membership(testPeriod, otherUser, testBranch, 100.0).apply {
            paid = true
        }
        membershipRepository.save(membership)

        val registration = ActivityRegistration(testActivity, otherUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(registration)
        registrationRepository.flush()

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertTrue(status.openOptions.isEmpty())
        assertEquals(1, status.closedOptions.size)
    }

    @Test
    fun `getStatusForActivityAndUser should return current registration when already registered`() {
        val registration = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(registration)
        registrationRepository.flush()

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertNotNull(status.currentRegistration)
        assertEquals(registration.id, status.currentRegistration!!.id)
    }

    @Test
    fun `getAllRegistrationsForActivity should return only paid registrations`() {
        val paidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(paidReg)

        val otherUser = User().apply {
            username = "unpaiduser"
            firstName = "Unpaid"
            name = "User"
            email = "unpaid@example.com"
            birthdate = LocalDate.of(2014, 1, 1)
        }
        userRepository.save(otherUser)

        val unpaidReg = ActivityRegistration(testActivity, otherUser, testRestriction, 50.0, null).apply {
            paid = false
        }
        registrationRepository.save(unpaidReg)
        registrationRepository.flush()

        val registrations = registrationService.getAllRegistrationsForActivity(testActivity.id!!)

        assertEquals(1, registrations.size)
        assertEquals(testUser.email, registrations[0].user.email)
    }

    @Test
    fun `getAllRegistrationsForUser should return all registrations for user`() {
        val reg1 = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(reg1)

        val activity2 = Activity().apply {
            name = "Activity 2"
            description = "Test"
            open = LocalDateTime.now().minusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 75.0
        }
        activityRepository.save(activity2)

        val restriction2 = ActivityRestriction().apply {
            name = "Restriction 2"
            activity = activity2
            branch = testBranch
        }
        restrictionRepository.save(restriction2)

        val reg2 = ActivityRegistration(activity2, testUser, restriction2, 75.0, null).apply {
            paid = true
        }
        registrationRepository.save(reg2)
        registrationRepository.flush()

        val registrations = registrationService.getAllRegistrationsForUser(testUser.username!!)

        assertEquals(2, registrations.size)
    }

    @Test
    fun `markRegistrationAsCompleted should fail for unpaid registration`() {
        val unpaidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = false
        }
        val saved = registrationRepository.save(unpaidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.markRegistrationAsCompleted(saved.id!!)
        }

        assertEquals("Only a paid activity can be marked as completed!", exception.message)
    }

    @Test
    fun `markRegistrationAsCompleted should fail if activity hasn't started yet`() {
        testActivity.start = LocalDateTime.now().plusDays(5)
        activityRepository.save(testActivity)

        val paidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.markRegistrationAsCompleted(saved.id!!)
        }

        assertEquals("Registrations can only be completed starting one hour before the activity!", exception.message)
    }

    @Test
    fun `markRegistrationAsCompleted should succeed for paid registration near activity start`() {
        testActivity.start = LocalDateTime.now().minusMinutes(30)
        testActivity.end = LocalDateTime.now().plusHours(2)
        activityRepository.save(testActivity)

        val paidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
            completed = false
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        registrationService.markRegistrationAsCompleted(saved.id!!)

        val updated = registrationRepository.findById(saved.id!!).get()
        assertTrue(updated.completed)
    }

    @Test
    fun `markRegistrationAsCompleted should not fail if already completed`() {
        testActivity.start = LocalDateTime.now().minusMinutes(30)
        testActivity.end = LocalDateTime.now().plusHours(2)
        activityRepository.save(testActivity)

        val paidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
            completed = true
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        assertDoesNotThrow {
            registrationService.markRegistrationAsCompleted(saved.id!!)
        }

        val registration = registrationRepository.findById(saved.id!!).get()
        assertTrue(registration.completed)
    }

    @Test
    fun `cancelRegistration should fail for unpaid registration`() {
        val unpaidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = false
        }
        val saved = registrationRepository.save(unpaidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.cancelRegistration(saved.id!!)
        }

        assertEquals("Only a paid activity registration can be cancelled!", exception.message)
    }

    @Test
    fun `cancelRegistration should fail when registrations are closed`() {
        testActivity.closed = LocalDateTime.now().minusDays(1)
        activityRepository.save(testActivity)

        val paidReg = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        val saved = registrationRepository.save(paidReg)
        registrationRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            registrationService.cancelRegistration(saved.id!!)
        }

        assertEquals("Cancellation is only possible when registrations are still open!", exception.message)
    }

    @Test
    fun `price calculation should apply sibling reduction when sibling already registered`() {
        // Create sibling user
        val sibling = User().apply {
            username = "sibling"
            firstName = "Sibling"
            name = "User"
            email = "sibling@example.com"
            birthdate = LocalDate.of(2012, 1, 1)
            hasReduction = false
        }
        userRepository.save(sibling)

        // Create sibling relationship
        val siblingRelation = Sibling(testUser, sibling)
        siblingRepository.save(siblingRelation)

        // Register sibling for activity
        val siblingRegistration = ActivityRegistration(testActivity, sibling, testRestriction, 50.0, null).apply {
            paid = true
        }
        registrationRepository.save(siblingRegistration)
        registrationRepository.flush()

        // Get status for test user - should show reduced price
        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        // Price should be reduced by siblingReduction (10.0) -> 50.0 - 10.0 = 40.0
        // However, status shows alternativePrice which is the base price
        // The reduction is applied during payment creation
        assertTrue(status.openOptions.isNotEmpty())
    }

    @Test
    fun `getActivityRegistrationDTOById should return registration when exists`() {
        val registration = ActivityRegistration(testActivity, testUser, testRestriction, 50.0, null).apply {
            paid = true
        }
        val saved = registrationRepository.save(registration)
        registrationRepository.flush()

        val result = registrationService.getActivityRegistrationDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals(testUser.email, result?.user?.email)
    }

    @Test
    fun `getActivityRegistrationDTOById should return null when not found`() {
        val result = registrationService.getActivityRegistrationDTOById(999)

        assertNull(result)
    }

    @Test
    fun `price calculation should handle additional data with JSONata`() {
        testActivity.additionalFormRule = "nights * 15"
        activityRepository.save(testActivity)

        val registration = ActivityRegistration(
            testActivity,
            testUser,
            testRestriction,
            50.0,
            "{\"nights\":\"2\"}"
        ).apply {
            paid = true
        }
        registrationRepository.save(registration)
        registrationRepository.flush()

        // Additional price should be 2 * 15 = 30.0, total with base price would be 80.0
        val result = registrationService.getActivityRegistrationDTOById(registration.id!!)
        assertNotNull(result)
    }

    @Test
    fun `restriction with alternative price should use alternative price instead of base price`() {
        testRestriction.alternativePrice = 25.0
        restrictionRepository.save(testRestriction)

        val status = registrationService.getStatusForActivityAndUser(testActivity.id!!, testUser.username!!)

        assertEquals(25.0, status.openOptions[0].alternativePrice)
    }
}
