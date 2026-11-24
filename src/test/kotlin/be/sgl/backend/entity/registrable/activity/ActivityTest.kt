package be.sgl.backend.entity.registrable.activity

import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother.activityRestriction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ActivityTest {

    private val branch = branch().id(1).name("Kapoenen").build()
    private val otherBranch = branch().id(2).name("Welpen").build()

    @Nested
    inner class GetRestrictionsForBranches {

        @Test
        fun `should return only restrictions of the given branches`() {
            // given
            val restriction = activityRestriction().id(1).branch(branch).build()
            val otherRestriction = activityRestriction().id(2).branch(otherBranch).build()
            val activity = activity().restrictions(restriction, otherRestriction).build()

            // when
            val result = activity.getRestrictionsForBranches(listOf(branch))

            // then
            assertThat(result).containsExactly(restriction)
        }

        @Test
        fun `should return no restrictions when no branches are given`() {
            // given
            val activity = activity().restrictions(activityRestriction().branch(branch).build()).build()

            // when
            val result = activity.getRestrictionsForBranches(emptyList())

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class GetBranchLimit {

        @Test
        fun `should return no branch limit when the branch only has a regular restriction`() {
            // given
            val activity = activity().restrictions(activityRestriction().branch(branch).alternativeLimit(5).build()).build()

            // when
            val limit = activity.getBranchLimit(branch)

            // then
            assertThat(limit).isNull()
        }

        @Test
        fun `should return the limit of the unnamed restriction when it serves as branch limit`() {
            // given
            val branchLimit = activityRestriction().id(3).branch(branch).alternativeLimit(10).build()
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("weekend").build(),
                activityRestriction().id(2).branch(branch).name("week").build(),
                branchLimit
            ).build()

            // when
            val limit = activity.getBranchLimit(branch)

            // then
            assertThat(limit).isEqualTo(branchLimit.alternativeLimit)
        }
    }

    @Nested
    inner class ValidateRestrictions {

        @Test
        fun `should accept a single restriction per branch`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).build(),
                activityRestriction().id(2).branch(otherBranch).build()
            ).build()

            // when / then
            assertThatCode { activity.validateRestrictions() }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept no restrictions`() {
            // given
            val activity = activity().build()

            // when / then
            assertThatCode { activity.validateRestrictions() }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept two named restrictions for the same branch`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("weekend").build(),
                activityRestriction().id(2).branch(branch).name("week").build()
            ).build()

            // when / then
            assertThatCode { activity.validateRestrictions() }.doesNotThrowAnyException()
        }

        @Test
        fun `should reject two restrictions for the same branch when one is unnamed`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("weekend").build(),
                activityRestriction().id(2).branch(branch).build()
            ).build()

            // when / then
            assertThatThrownBy { activity.validateRestrictions() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("All restrictions should be named for branch Kapoenen")
        }

        @Test
        fun `should accept named restrictions with a single unnamed branch limit`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("weekend").build(),
                activityRestriction().id(2).branch(branch).name("week").build(),
                activityRestriction().id(3).branch(branch).alternativeLimit(10).build()
            ).build()

            // when / then
            assertThatCode { activity.validateRestrictions() }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept more than two named restrictions for a branch without branch limit`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("a").build(),
                activityRestriction().id(2).branch(branch).name("b").build(),
                activityRestriction().id(3).branch(branch).name("c").build()
            ).build()

            // when / then
            assertThatCode { activity.validateRestrictions() }.doesNotThrowAnyException()
        }

        @Test
        fun `should reject more than two restrictions for a branch when multiple are unnamed`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("a").build(),
                activityRestriction().id(2).branch(branch).alternativeLimit(5).build(),
                activityRestriction().id(3).branch(branch).alternativeLimit(5).build()
            ).build()

            // when / then
            assertThatThrownBy { activity.validateRestrictions() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("At most one restriction (serving as branch limit) should be unnamed for branch Kapoenen")
        }

        @Test
        fun `should reject an unnamed branch limit without limit`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("a").build(),
                activityRestriction().id(2).branch(branch).name("b").build(),
                activityRestriction().id(3).branch(branch).build()
            ).build()

            // when / then
            assertThatThrownBy { activity.validateRestrictions() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The single unnamed restriction (serving as branch limit) for branch Kapoenen should have a limit")
        }

        @Test
        fun `should reject an unnamed branch limit with a price`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("a").build(),
                activityRestriction().id(2).branch(branch).name("b").build(),
                activityRestriction().id(3).branch(branch).alternativeLimit(5).alternativePrice(10.0).build()
            ).build()

            // when / then
            assertThatThrownBy { activity.validateRestrictions() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The single unnamed restriction (serving as branch limit) for branch Kapoenen should not have a price")
        }

        @Test
        fun `should reject an unnamed branch limit with a start date`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("a").build(),
                activityRestriction().id(2).branch(branch).name("b").build(),
                activityRestriction().id(3).branch(branch).alternativeLimit(5).alternativeStart(LocalDateTime.now()).build()
            ).build()

            // when / then
            assertThatThrownBy { activity.validateRestrictions() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The single unnamed restriction (serving as branch limit) for branch Kapoenen should not have a start date")
        }

        @Test
        fun `should reject an unnamed branch limit with an end date`() {
            // given
            val activity = activity().restrictions(
                activityRestriction().id(1).branch(branch).name("a").build(),
                activityRestriction().id(2).branch(branch).name("b").build(),
                activityRestriction().id(3).branch(branch).alternativeLimit(5).alternativeEnd(LocalDateTime.now()).build()
            ).build()

            // when / then
            assertThatThrownBy { activity.validateRestrictions() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The single unnamed restriction (serving as branch limit) for branch Kapoenen should not have an end date")
        }
    }
}
