package be.sgl.backend.entity.membership

import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.membership.MembershipRestrictionTestMother.membershipRestriction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MembershipRestrictionTest {

    private val period = membershipPeriod().start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2026, 8, 31)).build()

    @Test
    fun `should be a time restriction when an alternative start is set`() {
        // given
        val testee = membershipRestriction().alternativeStart(period.start).build()

        // when
        val result = testee.isTimeRestriction

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `should not be a time restriction when no alternative start is set`() {
        // given
        val testee = membershipRestriction().branch(branch().build()).build()

        // when
        val result = testee.isTimeRestriction

        // then
        assertThat(result).isFalse()
    }

    @Nested
    inner class ValidateTimeRestriction {

        @Test
        fun `should accept a time restriction when it starts on the period start`() {
            // given
            val testee = membershipRestriction().period(period).alternativeStart(period.start).alternativePrice(50.0).build()

            // when / then
            assertThatCode { testee.validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept a time restriction when it starts on the period end`() {
            // given
            val testee = membershipRestriction().period(period).alternativeStart(period.end).alternativePrice(50.0).build()

            // when / then
            assertThatCode { testee.validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept a time restriction when it is linked to a branch`() {
            // given
            val testee = membershipRestriction().period(period).branch(branch().build()).alternativeStart(period.start).alternativePrice(50.0).build()

            // when / then
            assertThatCode { testee.validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `should reject a time restriction when it starts before the period`() {
            // given
            val testee = membershipRestriction().period(period).alternativeStart(period.start.minusDays(1)).alternativePrice(50.0).build()

            // when / then
            assertThatThrownBy { testee.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of a time related restriction must be in its period boundaries!")
        }

        @Test
        fun `should reject a time restriction when it starts after the period`() {
            // given
            val testee = membershipRestriction().period(period).alternativeStart(period.end.plusDays(1)).alternativePrice(50.0).build()

            // when / then
            assertThatThrownBy { testee.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of a time related restriction must be in its period boundaries!")
        }

        @Test
        fun `should reject a time restriction when it has a registration limit`() {
            // given
            val testee = membershipRestriction().period(period).alternativeStart(period.start).alternativePrice(50.0).registrationLimit(10).build()

            // when / then
            assertThatThrownBy { testee.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A time related restriction can't have a registration limit!")
        }

        @Test
        fun `should reject a time restriction when it has no alternative price`() {
            // given
            val testee = membershipRestriction().period(period).alternativeStart(period.start).build()

            // when / then
            assertThatThrownBy { testee.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A time related restriction should have an alternative price!")
        }
    }

    @Nested
    inner class ValidateBranchRestriction {

        @Test
        fun `should accept a branch restriction when it only has an alternative price`() {
            // given
            val testee = membershipRestriction().period(period).branch(branch().build()).alternativePrice(50.0).build()

            // when / then
            assertThatCode { testee.validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept a branch restriction when it only has a registration limit`() {
            // given
            val testee = membershipRestriction().period(period).branch(branch().build()).registrationLimit(10).build()

            // when / then
            assertThatCode { testee.validate() }.doesNotThrowAnyException()
        }

        @Test
        fun `should reject a non time restriction when it has no branch`() {
            // given
            val testee = membershipRestriction().period(period).alternativePrice(50.0).build()

            // when / then
            assertThatThrownBy { testee.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("If a restriction is not time related, a linked branch is required!")
        }

        @Test
        fun `should reject a branch restriction when it has neither price nor limit`() {
            // given
            val testee = membershipRestriction().period(period).branch(branch().build()).build()

            // when / then
            assertThatThrownBy { testee.validate() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("If a restriction is not time related, it should have either an alternative price or a limit to be useful!")
        }
    }
}
