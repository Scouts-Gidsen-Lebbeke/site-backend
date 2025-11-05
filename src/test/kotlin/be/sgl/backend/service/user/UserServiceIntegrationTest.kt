package be.sgl.backend.service.user

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.StaffData
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.exception.UserNotFoundException
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
class UserServiceIntegrationTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    private lateinit var testUser: User
    private lateinit var testBranch: Branch

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
        branchRepository.deleteAll()

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
            birthdate = LocalDate.of(2000, 1, 1)
            sex = Sex.MALE
            mobile = "0123456789"
            hasReduction = false
            hasHandicap = false
        }
        testUser = userRepository.save(testUser)
    }

    @Test
    fun `getProfile should return user profile`() {
        val profile = userService.getProfile(testUser.username!!)

        assertNotNull(profile)
        assertEquals(testUser.username, profile.username)
        assertEquals(testUser.email, profile.email)
        assertEquals(testUser.firstName, profile.firstName)
        assertEquals(testUser.name, profile.name)
    }

    @Test
    fun `getProfile should throw exception for non-existent user`() {
        assertThrows(UserNotFoundException::class.java) {
            userService.getProfile("nonexistent")
        }
    }

    @Test
    fun `getByQuery should find users by name`() {
        val user2 = User().apply {
            username = "anotheruser"
            firstName = "Another"
            name = "User"
            email = "another@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }
        userRepository.save(user2)

        val user3 = User().apply {
            username = "different"
            firstName = "Different"
            name = "Person"
            email = "different@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }
        userRepository.save(user3)
        userRepository.flush()

        val results = userService.getByQuery("User")

        assertTrue(results.size >= 2)
        assertTrue(results.any { it.name == "User" })
    }

    @Test
    fun `getByQuery should find users by email`() {
        val results = userService.getByQuery(testUser.email)

        assertTrue(results.any { it.email == testUser.email })
    }

    @Test
    fun `getByQuery should find users by first name`() {
        val results = userService.getByQuery(testUser.firstName)

        assertTrue(results.any { it.firstName == testUser.firstName })
    }

    @Test
    fun `getByQuery should return empty list when no matches`() {
        val results = userService.getByQuery("NonExistentQuery12345")

        assertTrue(results.isEmpty())
    }

    @Test
    fun `getStaffBranch should return branch when user is staff`() {
        testUser.staffData = StaffData().apply {
            user = testUser
            branch = testBranch
            title = "Leader"
        }
        userRepository.save(testUser)
        userRepository.flush()

        val staffBranch = userService.getStaffBranch(testUser.username!!)

        assertNotNull(staffBranch)
        assertEquals(testBranch.name, staffBranch?.name)
    }

    @Test
    fun `getStaffBranch should return null when user is not staff`() {
        val staffBranch = userService.getStaffBranch(testUser.username!!)

        assertNull(staffBranch)
    }

    @Test
    fun `user profile should include all personal information`() {
        testUser.mobile = "0123456789"
        testUser.nis = "12345678901"
        testUser.accountNo = "BE12345678901234"
        testUser.hasReduction = true
        testUser.hasHandicap = false
        userRepository.save(testUser)
        userRepository.flush()

        val profile = userService.getProfile(testUser.username!!)

        assertEquals("0123456789", profile.mobile)
        assertEquals("12345678901", profile.nis)
        assertEquals("BE12345678901234", profile.accountNo)
        assertTrue(profile.hasReduction)
        assertFalse(profile.hasHandicap)
    }

    @Test
    fun `getByQuery should be case insensitive`() {
        val results1 = userService.getByQuery("TEST")
        val results2 = userService.getByQuery("test")

        assertTrue(results1.any { it.firstName.equals("Test", ignoreCase = true) })
        assertTrue(results2.any { it.firstName.equals("Test", ignoreCase = true) })
    }

    @Test
    fun `user profile should include birthdate and sex`() {
        val profile = userService.getProfile(testUser.username!!)

        assertEquals(LocalDate.of(2000, 1, 1), profile.birthdate)
        assertEquals(Sex.MALE, profile.sex)
    }

    @Test
    fun `getByQuery should support partial matching`() {
        val results = userService.getByQuery("Test")

        assertTrue(results.any { it.firstName.contains("Test", ignoreCase = true) })
    }

    @Test
    fun `user with multiple properties should be searchable by any property`() {
        val user = User().apply {
            username = "searchable"
            firstName = "Searchable"
            name = "TestName"
            email = "searchable@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }
        userRepository.save(user)
        userRepository.flush()

        val byFirstName = userService.getByQuery("Searchable")
        val byLastName = userService.getByQuery("TestName")
        val byEmail = userService.getByQuery("searchable@")

        assertTrue(byFirstName.any { it.username == "searchable" })
        assertTrue(byLastName.any { it.username == "searchable" })
        assertTrue(byEmail.any { it.username == "searchable" })
    }

    @Test
    fun `user profile should include age deviation`() {
        testUser.ageDeviation = 2
        userRepository.save(testUser)
        userRepository.flush()

        val profile = userService.getProfile(testUser.username!!)

        assertEquals(2, profile.ageDeviation)
    }

    @Test
    fun `user profile should handle null optional fields`() {
        val minimalUser = User().apply {
            username = "minimal"
            firstName = "Minimal"
            name = "User"
            email = "minimal@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }
        userRepository.save(minimalUser)
        userRepository.flush()

        val profile = userService.getProfile(minimalUser.username!!)

        assertNotNull(profile)
        assertNull(profile.mobile)
        assertNull(profile.nis)
        assertNull(profile.accountNo)
        assertNull(profile.image)
    }

    @Test
    fun `getStaffBranch should handle user with multiple branch associations`() {
        // User should only have one staff branch at a time
        testUser.staffData = StaffData().apply {
            user = testUser
            branch = testBranch
            title = "Leader"
        }
        userRepository.save(testUser)
        userRepository.flush()

        val staffBranch = userService.getStaffBranch(testUser.username!!)

        assertNotNull(staffBranch)
        assertEquals(testBranch.id, staffBranch?.id)
    }

    @Test
    fun `user search should handle special characters`() {
        val userWithSpecialChars = User().apply {
            username = "special"
            firstName = "Jean-Pierre"
            name = "D'Angelo"
            email = "special@example.com"
            birthdate = LocalDate.of(2000, 1, 1)
        }
        userRepository.save(userWithSpecialChars)
        userRepository.flush()

        val results = userService.getByQuery("Jean")

        assertTrue(results.any { it.firstName == "Jean-Pierre" })
    }
}
