package be.sgl.backend.entity.membership

import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.membership.MembershipRestrictionTestMother.membershipRestriction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MembershipPeriodTest {

    @Test
    fun `should return the limit of the restriction for the branch`() {
        // given
        val branch = branch().build()
        val limit = 20
        val testee = membershipPeriod().restrictions(membershipRestriction().branch(branch).registrationLimit(limit).build()).build()

        // when
        val result = testee.getLimitForBranch(branch)

        // then
        assertThat(result).isEqualTo(limit)
    }

    @Test
    fun `should return no limit when the branch has no restriction`() {
        // given
        val otherBranch = branch().id(2).build()
        val testee = membershipPeriod().restrictions(membershipRestriction().branch(otherBranch).registrationLimit(20).build()).build()

        // when
        val result = testee.getLimitForBranch(branch().build())

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should return no limit when the period has no restrictions`() {
        // given
        val testee = membershipPeriod().build()

        // when
        val result = testee.getLimitForBranch(branch().build())

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should return the branch limit when a time branch restriction for the same branch comes first`() {
        // given
        val branch = branch().build()
        val limit = 20
        val timeBranchRestriction = membershipRestriction().branch(branch).alternativeStart(LocalDate.of(2025, 10, 1)).alternativePrice(60.0).build()
        val branchRestriction = membershipRestriction().branch(branch).registrationLimit(limit).build()
        val testee = membershipPeriod().restrictions(timeBranchRestriction, branchRestriction).build()

        // when
        val result = testee.getLimitForBranch(branch)

        // then
        assertThat(result).isEqualTo(limit)
    }

    @Test
    fun `should format the period with belgian dates`() {
        // given
        val testee = membershipPeriod().start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2026, 8, 31)).build()

        // when
        val result = testee.toString()

        // then
        assertThat(result).isEqualTo("01/09/2025 - 31/08/2026")
    }
}
