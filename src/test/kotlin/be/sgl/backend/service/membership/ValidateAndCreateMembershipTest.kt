package be.sgl.backend.service.membership

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.membership.MembershipRestrictionTestMother.membershipRestriction
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.SiblingRelationTestMother.siblingRelation
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.util.reducePrice
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ValidateAndCreateMembershipTest {

    @Mock
    lateinit var membershipRepository: MembershipRepository
    @Mock
    lateinit var branchRepository: BranchRepository
    @Mock
    lateinit var siblingRepository: SiblingRepository
    @InjectMocks
    lateinit var testee: ValidateAndCreateMembership

    private val periodStart = LocalDate.of(2025, 9, 1)
    private val periodEnd = LocalDate.of(2026, 8, 31)
    // the age of a user is determined on the last day of the year the period ends in
    private val ageReferenceDate = LocalDate.of(2026, 12, 31)
    private val at = LocalDate.of(2025, 10, 15)
    private val birthdate = LocalDate.of(2016, 5, 1)
    private val branch = branch().build()

    private fun period() = membershipPeriod().start(periodStart).end(periodEnd)

    private fun givenBranchFor(user: User, branch: Branch = this.branch) {
        given(branchRepository.getPossibleBranchesForSexAndAge(user.sex, user.getAge(ageReferenceDate) + user.ageDeviation))
            .willReturn(listOf(branch))
    }

    @Nested
    inner class BranchSelection {

        @Test
        fun `should link the membership to the first possible branch for the user`() {
            // given
            val user = user().birthdate(birthdate).sex(Sex.FEMALE).build()
            val period = period().build()
            val otherBranch = branch().id(2).build()
            given(branchRepository.getPossibleBranchesForSexAndAge(Sex.FEMALE, 10)).willReturn(listOf(branch, otherBranch))

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.user).isSameAs(user)
            assertThat(membership.period).isSameAs(period)
            assertThat(membership.branch).isSameAs(branch)
        }

        @Test
        fun `should add the age deviation when determining the branch`() {
            // given
            val user = user().birthdate(birthdate).sex(Sex.MALE).ageDeviation(-1).build()
            given(branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 9)).willReturn(listOf(branch))

            // when
            val membership = testee.execute(period().build(), user, at)

            // then
            assertThat(membership.branch).isSameAs(branch)
        }

        @Test
        fun `should throw when no branch matches the age and sex of the user`() {
            // given
            val user = user().birthdate(birthdate).build()
            given(branchRepository.getPossibleBranchesForSexAndAge(user.sex, 10)).willReturn(emptyList())

            // when / then
            assertThatThrownBy { testee.execute(period().build(), user, at) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No active branch can be linked to a user of this age and sex!")
        }
    }

    @Nested
    inner class Limits {

        private val limit = 10

        @Test
        fun `should throw when the period limit is reached`() {
            // given
            val period = period().registrationLimit(limit).build()
            given(membershipRepository.countByPeriod(period)).willReturn(limit)

            // when / then
            assertThatThrownBy { testee.execute(period, user().build(), at) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This period already has its maximum number of members!")
            then(branchRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should create the membership when one spot is left in the period`() {
            // given
            val user = user().birthdate(birthdate).build()
            val period = period().registrationLimit(limit).build()
            given(membershipRepository.countByPeriod(period)).willReturn(limit - 1)
            givenBranchFor(user)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(period.price)
        }

        @Test
        fun `should throw when the branch limit is reached`() {
            // given
            val user = user().birthdate(birthdate).build()
            val period = period().restrictions(membershipRestriction().branch(branch).registrationLimit(limit).build()).build()
            givenBranchFor(user)
            given(membershipRepository.countByPeriodAndBranch(period, branch)).willReturn(limit)

            // when / then
            assertThatThrownBy { testee.execute(period, user, at) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No more free membership spots for this branch!")
        }

        @Test
        fun `should create the membership when one spot is left in the branch`() {
            // given
            val user = user().birthdate(birthdate).build()
            val period = period().restrictions(membershipRestriction().branch(branch).registrationLimit(limit).build()).build()
            givenBranchFor(user)
            given(membershipRepository.countByPeriodAndBranch(period, branch)).willReturn(limit - 1)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.branch).isSameAs(branch)
        }

        @Test
        @Disabled("Possible bug: MembershipPeriod.getLimitForBranch returns the limit of the first branch restriction, which can be a limitless time-branch restriction")
        fun `should throw when the branch limit is reached and a time branch restriction comes first`() {
            // given
            val user = user().birthdate(birthdate).build()
            val timeBranchRestriction = membershipRestriction().branch(branch).alternativeStart(periodStart.plusDays(10)).alternativePrice(70.0).build()
            val limitRestriction = membershipRestriction().branch(branch).registrationLimit(limit).build()
            val period = period().restrictions(timeBranchRestriction, limitRestriction).build()
            givenBranchFor(user)
            given(membershipRepository.countByPeriodAndBranch(period, branch)).willReturn(limit)

            // when / then
            assertThatThrownBy { testee.execute(period, user, at) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No more free membership spots for this branch!")
        }
    }

    @Nested
    inner class BasePrice {

        private val timePrice = 120.0
        private val branchPrice = 80.0
        private val branchTimePrice = 90.0

        private fun execute(period: MembershipPeriod): Double {
            val user = user().birthdate(birthdate).build()
            givenBranchFor(user)
            return testee.execute(period, user, at).price
        }

        @Test
        fun `should use the period price when there are no restrictions`() {
            // given
            val period = period().price(100.0).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(period.price)
        }

        @Test
        fun `should use the time restriction price when it started before the registration date`() {
            // given
            val period = period().restrictions(membershipRestriction().alternativeStart(at.minusDays(1)).alternativePrice(timePrice).build()).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(timePrice)
        }

        @Test
        fun `should use the period price when the time restriction starts on the registration date`() {
            // given
            val period = period().restrictions(membershipRestriction().alternativeStart(at).alternativePrice(timePrice).build()).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(period.price)
        }

        @Test
        fun `should use the period price when the time restriction starts after the registration date`() {
            // given
            val period = period().restrictions(membershipRestriction().alternativeStart(at.plusDays(1)).alternativePrice(timePrice).build()).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(period.price)
        }

        @Test
        fun `should use the most recently started time restriction when several have started`() {
            // given
            val laterPrice = 150.0
            val period = period().restrictions(
                membershipRestriction().alternativeStart(at.minusDays(10)).alternativePrice(timePrice).build(),
                membershipRestriction().alternativeStart(at.minusDays(2)).alternativePrice(laterPrice).build(),
                membershipRestriction().alternativeStart(at.minusDays(5)).alternativePrice(130.0).build()
            ).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(laterPrice)
        }

        @Test
        fun `should use the branch restriction price when the branch has one`() {
            // given
            val period = period().restrictions(membershipRestriction().branch(branch).alternativePrice(branchPrice).build()).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(branchPrice)
        }

        @Test
        fun `should use the period price when the branch restriction has no price`() {
            // given
            val period = period().restrictions(membershipRestriction().branch(branch).registrationLimit(50).build()).build()
            given(membershipRepository.countByPeriodAndBranch(period, branch)).willReturn(0)

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(period.price)
        }

        @Test
        fun `should prefer the started branch time restriction over the branch restriction`() {
            // given
            val period = period().restrictions(
                membershipRestriction().branch(branch).alternativePrice(branchPrice).build(),
                membershipRestriction().branch(branch).alternativeStart(at.minusDays(1)).alternativePrice(branchTimePrice).build()
            ).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(branchTimePrice)
        }

        @Test
        fun `should use the branch restriction price when the branch time restriction has not started`() {
            // given
            val period = period().restrictions(
                membershipRestriction().branch(branch).alternativePrice(branchPrice).build(),
                membershipRestriction().branch(branch).alternativeStart(at.plusDays(1)).alternativePrice(branchTimePrice).build()
            ).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(branchPrice)
        }

        @Test
        fun `should ignore general time restrictions when the branch has its own restriction`() {
            // given
            val period = period().restrictions(
                membershipRestriction().alternativeStart(at.minusDays(1)).alternativePrice(timePrice).build(),
                membershipRestriction().branch(branch).alternativePrice(branchPrice).build()
            ).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(branchPrice)
        }

        @Test
        @Disabled("Possible bug: time-branch restrictions of other branches are applied to users of a branch without restrictions")
        fun `should ignore the time restriction of another branch when the branch has no restrictions`() {
            // given
            val otherBranch = branch().id(2).build()
            val period = period().restrictions(
                membershipRestriction().branch(otherBranch).alternativeStart(at.minusDays(1)).alternativePrice(branchTimePrice).build()
            ).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(period.price)
        }

        @Test
        @Disabled("Possible bug: NoSuchElementException from first() when the branch only has time-branch restrictions that have not started yet")
        fun `should use the period price when the branch only has a time restriction that has not started`() {
            // given
            val period = period().restrictions(
                membershipRestriction().branch(branch).alternativeStart(at.plusDays(1)).alternativePrice(branchTimePrice).build()
            ).build()

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(period.price)
        }

        @Test
        @Disabled("Possible bug: a branch restriction that only sets a limit discards the started general time restriction price")
        fun `should use the general time restriction price when the branch restriction only has a limit`() {
            // given
            val period = period().restrictions(
                membershipRestriction().alternativeStart(at.minusDays(1)).alternativePrice(timePrice).build(),
                membershipRestriction().branch(branch).registrationLimit(50).build()
            ).build()
            given(membershipRepository.countByPeriodAndBranch(period, branch)).willReturn(0)

            // when
            val price = execute(period)

            // then
            assertThat(price).isEqualTo(timePrice)
        }
    }

    @Nested
    inner class Reductions {

        private val siblingReduction = 30.0

        @Test
        fun `should divide the price by the reduction factor when the user has a reduction`() {
            // given
            val user = user().birthdate(birthdate).hasReduction().build()
            val period = period().price(100.0).reductionFactor(3.0).siblingReduction(siblingReduction).build()
            givenBranchFor(user)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(period.price.reducePrice(period.reductionFactor)).isEqualTo(33.33)
            then(siblingRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should subtract the sibling reduction when a sibling already has a membership`() {
            // given
            val user = user().birthdate(birthdate).build()
            val relation = siblingRelation().user(user).build()
            val period = period().price(100.0).siblingReduction(siblingReduction).build()
            givenBranchFor(user)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(membershipRepository.existsByPeriodAndUser(period, relation.sibling)).willReturn(true)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(period.price - period.siblingReduction)
        }

        @Test
        fun `should not go below zero when the sibling reduction exceeds the price`() {
            // given
            val user = user().birthdate(birthdate).build()
            val relation = siblingRelation().user(user).build()
            val period = period().price(20.0).siblingReduction(siblingReduction).build()
            givenBranchFor(user)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(membershipRepository.existsByPeriodAndUser(period, relation.sibling)).willReturn(true)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(0.0)
        }

        @Test
        fun `should not apply the sibling reduction when the sibling has no membership`() {
            // given
            val user = user().birthdate(birthdate).build()
            val relation = siblingRelation().user(user).build()
            val period = period().siblingReduction(siblingReduction).build()
            givenBranchFor(user)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(membershipRepository.existsByPeriodAndUser(period, relation.sibling)).willReturn(false)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(period.price)
        }

        @Test
        fun `should not apply the sibling reduction when the sibling has a reduction`() {
            // given
            val user = user().birthdate(birthdate).build()
            val sibling = user().id(2).hasReduction().build()
            val period = period().siblingReduction(siblingReduction).build()
            givenBranchFor(user)
            given(siblingRepository.getByUser(user)).willReturn(listOf(siblingRelation().user(user).sibling(sibling).build()))

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(period.price)
            then(membershipRepository).should(org.mockito.Mockito.never()).existsByPeriodAndUser(period, sibling)
        }

        @Test
        fun `should apply the sibling reduction on the restriction price`() {
            // given
            val user = user().birthdate(birthdate).build()
            val relation = siblingRelation().user(user).build()
            val branchPrice = 80.0
            val period = period().siblingReduction(siblingReduction)
                .restrictions(membershipRestriction().branch(branch).alternativePrice(branchPrice).build()).build()
            givenBranchFor(user)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(membershipRepository.existsByPeriodAndUser(period, relation.sibling)).willReturn(true)

            // when
            val membership = testee.execute(period, user, at)

            // then
            assertThat(membership.price).isEqualTo(branchPrice - siblingReduction)
        }
    }
}
