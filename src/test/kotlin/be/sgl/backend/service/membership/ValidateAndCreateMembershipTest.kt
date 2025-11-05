package be.sgl.backend.service.membership

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.SiblingRepository
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
class ValidateAndCreateMembershipTest {

    @Autowired
    private lateinit var validateAndCreateMembership: ValidateAndCreateMembership

    @Autowired
    private lateinit var membershipRepository: MembershipRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Autowired
    private lateinit var siblingRepository: SiblingRepository

    private lateinit var period: MembershipPeriod
    private lateinit var branch: Branch
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        // Create a test branch
        branch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test branch description"
            law = "Test law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        branch = branchRepository.save(branch)

        // Create a test membership period
        period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
            reductionFactor = 3.0
            siblingReduction = 10.0
        }

        // Create a test user
        user = User().apply {
            name = "Doe"
            firstName = "John"
            email = "john.doe@example.com"
            birthdate = LocalDate.of(2017, 5, 15) // 7 years old
            sex = Sex.MALE
        }
    }

    @Test
    fun `execute should create membership with base price for regular user`() {
        val membership = validateAndCreateMembership.execute(period, user)

        assertNotNull(membership)
        assertEquals(user, membership.user)
        assertEquals(period, membership.period)
        assertEquals(branch, membership.branch)
        assertEquals(100.0, membership.price)
    }

    @Test
    fun `execute should apply reduction for user with hasReduction flag`() {
        user.hasReduction = true

        val membership = validateAndCreateMembership.execute(period, user)

        assertEquals(33.33, membership.price) // 100 / 3 rounded to 2 decimals
    }

    @Test
    fun `execute should apply sibling reduction when sibling has membership`() {
        // Create sibling
        val sibling = User().apply {
            name = "Doe"
            firstName = "Jane"
            email = "jane.doe@example.com"
            birthdate = LocalDate.of(2016, 3, 10)
            sex = Sex.FEMALE
            hasReduction = false
        }

        // Save sibling first
        siblingRepository.flush()

        // Create membership for sibling
        val siblingMembership = validateAndCreateMembership.execute(period, sibling)
        membershipRepository.save(siblingMembership)
        membershipRepository.flush()

        // Register sibling relationship
        val siblingRelation = be.sgl.backend.entity.user.SiblingRelation().apply {
            this.user = user
            this.sibling = sibling
        }
        siblingRepository.save(siblingRelation)
        siblingRepository.flush()

        val membership = validateAndCreateMembership.execute(period, user)

        assertEquals(90.0, membership.price) // 100 - 10 sibling reduction
    }

    @Test
    fun `execute should not apply sibling reduction if sibling has reduction flag`() {
        val sibling = User().apply {
            name = "Doe"
            firstName = "Jane"
            email = "jane.doe@example.com"
            birthdate = LocalDate.of(2016, 3, 10)
            sex = Sex.FEMALE
            hasReduction = true // Sibling has reduction
        }

        siblingRepository.flush()

        val siblingMembership = validateAndCreateMembership.execute(period, sibling)
        membershipRepository.save(siblingMembership)
        membershipRepository.flush()

        val siblingRelation = be.sgl.backend.entity.user.SiblingRelation().apply {
            this.user = user
            this.sibling = sibling
        }
        siblingRepository.save(siblingRelation)
        siblingRepository.flush()

        val membership = validateAndCreateMembership.execute(period, user)

        assertEquals(100.0, membership.price) // Full price, no sibling reduction
    }

    @Test
    fun `execute should respect period registration limit`() {
        period.registrationLimit = 1

        // Create first membership
        val firstUser = User().apply {
            name = "Smith"
            firstName = "Alice"
            email = "alice@example.com"
            birthdate = LocalDate.of(2017, 1, 1)
            sex = Sex.FEMALE
        }
        val firstMembership = validateAndCreateMembership.execute(period, firstUser)
        membershipRepository.save(firstMembership)
        membershipRepository.flush()

        // Try to create second membership - should fail
        val exception = assertThrows(IllegalStateException::class.java) {
            validateAndCreateMembership.execute(period, user)
        }

        assertEquals("This period already has its maximum number of members!", exception.message)
    }

    @Test
    fun `execute should respect branch-specific registration limit`() {
        val restriction = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.registrationLimit = 1
        }
        period.restrictions.add(restriction)

        // Create first membership for this branch
        val firstUser = User().apply {
            name = "Smith"
            firstName = "Alice"
            email = "alice@example.com"
            birthdate = LocalDate.of(2017, 1, 1)
            sex = Sex.FEMALE
        }
        val firstMembership = validateAndCreateMembership.execute(period, firstUser)
        membershipRepository.save(firstMembership)
        membershipRepository.flush()

        // Try to create second membership - should fail
        val exception = assertThrows(IllegalStateException::class.java) {
            validateAndCreateMembership.execute(period, user)
        }

        assertEquals("No more free membership spots for this branch!", exception.message)
    }

    @Test
    fun `execute should apply time-based price restriction`() {
        val earlyBirdPrice = 80.0
        val earlyBirdDate = LocalDate.of(2024, 7, 1)

        val restriction = MembershipRestriction().apply {
            this.period = period
            this.alternativeStart = earlyBirdDate
            this.alternativePrice = earlyBirdPrice
        }
        period.restrictions.add(restriction)

        val membershipBeforeDeadline = validateAndCreateMembership.execute(
            period, user, at = LocalDate.of(2024, 7, 15)
        )

        assertEquals(earlyBirdPrice, membershipBeforeDeadline.price)

        val membershipAfterDeadline = validateAndCreateMembership.execute(
            period, user, at = LocalDate.of(2024, 8, 15)
        )

        assertEquals(100.0, membershipAfterDeadline.price) // Back to base price
    }

    @Test
    fun `execute should apply branch-specific alternative price`() {
        val branchSpecificPrice = 75.0

        val restriction = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.alternativePrice = branchSpecificPrice
        }
        period.restrictions.add(restriction)

        val membership = validateAndCreateMembership.execute(period, user)

        assertEquals(branchSpecificPrice, membership.price)
    }

    @Test
    fun `execute should throw exception when no branch matches user age and sex`() {
        // Create user that doesn't fit any branch
        val tooOldUser = User().apply {
            name = "Old"
            firstName = "Person"
            email = "old@example.com"
            birthdate = LocalDate.of(2000, 1, 1) // Too old for our test branch
            sex = Sex.MALE
        }

        val exception = assertThrows(IllegalStateException::class.java) {
            validateAndCreateMembership.execute(period, tooOldUser)
        }

        assertEquals("No active branch can be linked to a user of this age and sex!", exception.message)
    }

    @Test
    fun `execute should consider user age deviation`() {
        // User is normally 7 years old, but with deviation of 2, effectively 9
        user.ageDeviation = 2

        // This should fail because effective age (9) exceeds branch maximum (8)
        val exception = assertThrows(IllegalStateException::class.java) {
            validateAndCreateMembership.execute(period, user)
        }

        assertEquals("No active branch can be linked to a user of this age and sex!", exception.message)
    }

    @Test
    fun `execute should calculate age at end of period`() {
        // User born in May 2017, will be 8 at end of period (Aug 2025)
        val youngUser = User().apply {
            name = "Young"
            firstName = "Person"
            email = "young@example.com"
            birthdate = LocalDate.of(2017, 5, 15)
            sex = Sex.MALE
        }

        val membership = validateAndCreateMembership.execute(period, youngUser)

        assertNotNull(membership)
        assertEquals(branch, membership.branch) // Should still fit in branch
    }

    @Test
    fun `execute should prefer most recent time restriction`() {
        val firstRestriction = MembershipRestriction().apply {
            this.period = period
            this.alternativeStart = LocalDate.of(2024, 6, 1)
            this.alternativePrice = 90.0
        }
        val secondRestriction = MembershipRestriction().apply {
            this.period = period
            this.alternativeStart = LocalDate.of(2024, 7, 1)
            this.alternativePrice = 85.0
        }
        period.restrictions.addAll(listOf(firstRestriction, secondRestriction))

        val membership = validateAndCreateMembership.execute(
            period, user, at = LocalDate.of(2024, 7, 15)
        )

        assertEquals(85.0, membership.price) // Should use most recent restriction
    }
}
