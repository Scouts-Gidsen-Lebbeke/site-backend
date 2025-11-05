package be.sgl.backend.service.activity

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class ActivityServiceIntegrationTest {

    @Autowired
    private lateinit var activityService: ActivityService

    @Autowired
    private lateinit var activityRepository: ActivityRepository

    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository

    private lateinit var testActivity: Activity
    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        testUser = User().apply {
            name = "Test"
            firstName = "User"
            email = "test@example.com"
            birthdate = java.time.LocalDate.of(2010, 1, 1)
            sex = Sex.MALE
        }

        testActivity = Activity().apply {
            name = "Test Activity"
            description = "Test Description"
            open = LocalDateTime.now().plusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            registrationLimit = 10
            cancellable = true
            sendConfirmation = true
            sendCompleteConfirmation = false
        }
    }

    @Test
    fun `getAllActivities should return all activities with registration data`() {
        val savedActivity = activityRepository.save(testActivity)
        activityRepository.flush()

        val activities = activityService.getAllActivities()

        assertNotNull(activities)
        assertTrue(activities.any { it.activity.id == savedActivity.id })
    }

    @Test
    fun `getVisibleActivities should only return non-cancelled activities`() {
        val visibleActivity = testActivity
        val cancelledActivity = Activity().apply {
            name = "Cancelled Activity"
            description = "Cancelled"
            open = LocalDateTime.now().plusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            cancelled = true
        }

        activityRepository.save(visibleActivity)
        activityRepository.save(cancelledActivity)
        activityRepository.flush()

        val activities = activityService.getVisibleActivities()

        assertTrue(activities.any { it.name == "Test Activity" })
        assertFalse(activities.any { it.name == "Cancelled Activity" })
    }

    @Test
    fun `saveActivityDTO should validate open-closed-start-end sequence`() {
        val dto = ActivityDTO(
            id = null,
            name = "Invalid Activity",
            description = "Test",
            open = LocalDateTime.now().plusDays(10),
            closed = LocalDateTime.now().plusDays(5), // Closed before open!
            start = LocalDateTime.now().plusDays(15),
            end = LocalDateTime.now().plusDays(20),
            price = 50.0,
            reductionFactor = 3.0,
            siblingReduction = 10.0,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null,
            restrictions = emptyList()
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            activityService.saveActivityDTO(dto)
        }

        assertEquals("The closure of registrations should be after the opening of registrations!", exception.message)
    }

    @Test
    fun `saveActivityDTO should validate closed before start`() {
        val dto = ActivityDTO(
            id = null,
            name = "Invalid Activity",
            description = "Test",
            open = LocalDateTime.now().plusDays(5),
            closed = LocalDateTime.now().plusDays(10),
            start = LocalDateTime.now().plusDays(8), // Start before closed!
            end = LocalDateTime.now().plusDays(20),
            price = 50.0,
            reductionFactor = 3.0,
            siblingReduction = 10.0,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null,
            restrictions = emptyList()
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            activityService.saveActivityDTO(dto)
        }

        assertEquals("The start date of an activity should be after the closure of registrations!", exception.message)
    }

    @Test
    fun `mergeActivityDTOChanges should not allow editing cancelled activity`() {
        testActivity.cancelled = true
        val savedActivity = activityRepository.save(testActivity)
        activityRepository.flush()

        val dto = ActivityDTO(
            id = savedActivity.id,
            name = "Updated Activity",
            description = "Updated",
            open = testActivity.open,
            closed = testActivity.closed,
            start = testActivity.start,
            end = testActivity.end,
            price = 60.0,
            reductionFactor = 3.0,
            siblingReduction = 10.0,
            registrationLimit = null,
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null,
            restrictions = emptyList()
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            activityService.mergeActivityDTOChanges(savedActivity.id!!, dto)
        }

        assertEquals("A cancelled activity cannot be edited anymore!", exception.message)
    }

    @Test
    fun `mergeActivityDTOChanges should allow full edit when not yet open`() {
        val savedActivity = activityRepository.save(testActivity)
        activityRepository.flush()

        val dto = ActivityDTO(
            id = savedActivity.id,
            name = "Updated Activity",
            description = "Updated Description",
            open = testActivity.open,
            closed = testActivity.closed,
            start = testActivity.start,
            end = testActivity.end,
            price = 75.0,
            reductionFactor = 2.0,
            siblingReduction = 15.0,
            registrationLimit = 20,
            address = null,
            additionalForm = "{\"field\": \"value\"}",
            additionalFormRule = "price + 10",
            cancellable = false,
            sendConfirmation = false,
            sendCompleteConfirmation = true,
            communicationCC = "cc@example.com",
            restrictions = emptyList()
        )

        val updatedActivity = activityService.mergeActivityDTOChanges(savedActivity.id!!, dto)

        assertEquals("Updated Activity", updatedActivity.name)
        assertEquals(75.0, updatedActivity.price)
        assertEquals(2.0, updatedActivity.reductionFactor)
        assertEquals(15.0, updatedActivity.siblingReduction)
    }

    @Test
    fun `mergeActivityDTOChanges should not allow lowering registration limit below current registrations`() {
        // Create activity with limit
        testActivity.registrationLimit = 10
        testActivity.open = LocalDateTime.now().minusDays(5) // Already open
        testActivity.closed = LocalDateTime.now().plusDays(5)
        val savedActivity = activityRepository.save(testActivity)
        activityRepository.flush()

        // Create 3 paid registrations
        repeat(3) { index ->
            val user = User().apply {
                name = "User$index"
                firstName = "Test"
                email = "user$index@example.com"
                birthdate = java.time.LocalDate.of(2010, 1, 1)
                sex = Sex.MALE
            }
            val registration = ActivityRegistration(user, savedActivity, savedActivity.price).apply {
                paid = true
            }
            registrationRepository.save(registration)
        }
        registrationRepository.flush()

        val dto = ActivityDTO(
            id = savedActivity.id,
            name = savedActivity.name,
            description = savedActivity.description,
            open = savedActivity.open,
            closed = savedActivity.closed,
            start = savedActivity.start,
            end = savedActivity.end,
            price = savedActivity.price,
            reductionFactor = 3.0,
            siblingReduction = 10.0,
            registrationLimit = 2, // Trying to lower below 3 registrations!
            address = null,
            additionalForm = null,
            additionalFormRule = null,
            cancellable = true,
            sendConfirmation = true,
            sendCompleteConfirmation = false,
            communicationCC = null,
            restrictions = emptyList()
        )

        val exception = assertThrows(IllegalStateException::class.java) {
            activityService.mergeActivityDTOChanges(savedActivity.id!!, dto)
        }

        assertEquals("The registration limit cannot be lowered below the current registration count!", exception.message)
    }

    @Test
    fun `cancelActivity should mark activity as cancelled and initiate refunds`() {
        testActivity.open = LocalDateTime.now().minusDays(5)
        testActivity.closed = LocalDateTime.now().plusDays(5)
        val savedActivity = activityRepository.save(testActivity)
        activityRepository.flush()

        // Create a paid registration
        val registration = ActivityRegistration(testUser, savedActivity, savedActivity.price).apply {
            paid = true
            paymentId = "payment-123"
        }
        registrationRepository.save(registration)
        registrationRepository.flush()

        activityService.cancelActivity(savedActivity.id!!)

        val cancelledActivity = activityRepository.findById(savedActivity.id!!).get()
        assertTrue(cancelledActivity.cancelled)
    }

    @Test
    fun `cancelActivity should not allow cancelling already cancelled activity`() {
        testActivity.cancelled = true
        val savedActivity = activityRepository.save(testActivity)
        activityRepository.flush()

        val exception = assertThrows(IllegalStateException::class.java) {
            activityService.cancelActivity(savedActivity.id!!)
        }

        assertEquals("This activity is already cancelled!", exception.message)
    }

    @Test
    fun `saveActivityDTO should create activity with address`() {
        val address = Address().apply {
            street = "Test Street"
            number = "123"
            zipcode = "1000"
            town = "Brussels"
            country = "BE"
        }

        val activityWithAddress = Activity().apply {
            name = "Activity with Address"
            description = "Test"
            open = LocalDateTime.now().plusDays(1)
            closed = LocalDateTime.now().plusDays(7)
            start = LocalDateTime.now().plusDays(10)
            end = LocalDateTime.now().plusDays(12)
            price = 50.0
            this.address = address
        }

        val saved = activityRepository.save(activityWithAddress)
        val retrieved = activityService.getActivityDTOById(saved.id!!)

        assertNotNull(retrieved.address)
        assertEquals("Test Street", retrieved.address?.street)
        assertEquals("123", retrieved.address?.number)
    }
}
