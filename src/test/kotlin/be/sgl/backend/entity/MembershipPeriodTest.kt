package be.sgl.backend.entity

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipRestriction
import be.sgl.backend.entity.user.Sex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MembershipPeriodTest {

    @Test
    fun `getLimitForBranch should return limit when restriction exists for branch`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val branch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val restriction = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.registrationLimit = 50
        }

        period.restrictions.add(restriction)

        val limit = period.getLimitForBranch(branch)

        assertEquals(50, limit)
    }

    @Test
    fun `getLimitForBranch should return null when no restriction for branch`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val branch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val limit = period.getLimitForBranch(branch)

        assertNull(limit)
    }

    @Test
    fun `toString should format dates in Belgian format`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val result = period.toString()

        assertEquals("01/09/2024 - 31/08/2025", result)
    }

    @Test
    fun `validateRestrictions should pass when no restrictions`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        assertDoesNotThrow {
            period.validateRestrictions()
        }
    }

    @Test
    fun `validateRestrictions should pass with valid time restrictions`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val timeRestriction1 = MembershipRestriction().apply {
            this.period = period
            this.alternativeStart = LocalDate.of(2024, 7, 1)
            this.alternativePrice = 80.0
        }

        val timeRestriction2 = MembershipRestriction().apply {
            this.period = period
            this.alternativeStart = LocalDate.of(2024, 8, 1)
            this.alternativePrice = 90.0
        }

        period.restrictions.addAll(listOf(timeRestriction1, timeRestriction2))

        assertDoesNotThrow {
            period.validateRestrictions()
        }
    }

    @Test
    fun `validateRestrictions should pass with one branch restriction per branch`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val branch1 = Branch().apply {
            name = "Branch 1"
            email = "branch1@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val branch2 = Branch().apply {
            name = "Branch 2"
            email = "branch2@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val restriction1 = MembershipRestriction().apply {
            this.period = period
            this.branch = branch1
            this.registrationLimit = 50
        }

        val restriction2 = MembershipRestriction().apply {
            this.period = period
            this.branch = branch2
            this.registrationLimit = 60
        }

        period.restrictions.addAll(listOf(restriction1, restriction2))

        assertDoesNotThrow {
            period.validateRestrictions()
        }
    }

    @Test
    fun `validateRestrictions should fail with multiple non-time restrictions for same branch`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val branch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val restriction1 = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.registrationLimit = 50
            // No alternativeStart, so not a time restriction
        }

        val restriction2 = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.alternativePrice = 75.0
            // No alternativeStart, so not a time restriction
        }

        period.restrictions.addAll(listOf(restriction1, restriction2))

        val exception = assertThrows(IllegalStateException::class.java) {
            period.validateRestrictions()
        }

        assertEquals("A branch should at most have one single non-time related restriction!", exception.message)
    }

    @Test
    fun `validateRestrictions should allow time restrictions plus one regular restriction per branch`() {
        val period = MembershipPeriod().apply {
            start = LocalDate.of(2024, 9, 1)
            end = LocalDate.of(2025, 8, 31)
            price = 100.0
        }

        val branch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val branchRestriction = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.registrationLimit = 50
        }

        val timeRestriction1 = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.alternativeStart = LocalDate.of(2024, 7, 1)
            this.alternativePrice = 80.0
        }

        val timeRestriction2 = MembershipRestriction().apply {
            this.period = period
            this.branch = branch
            this.alternativeStart = LocalDate.of(2024, 8, 1)
            this.alternativePrice = 90.0
        }

        period.restrictions.addAll(listOf(branchRestriction, timeRestriction1, timeRestriction2))

        assertDoesNotThrow {
            period.validateRestrictions()
        }
    }

    @Test
    fun `period should have default values`() {
        val period = MembershipPeriod()

        assertEquals(0.0, period.price)
        assertNull(period.registrationLimit)
        assertEquals(3.0, period.reductionFactor)
        assertEquals(0.0, period.siblingReduction)
        assertNotNull(period.restrictions)
        assertTrue(period.restrictions.isEmpty())
    }
}
