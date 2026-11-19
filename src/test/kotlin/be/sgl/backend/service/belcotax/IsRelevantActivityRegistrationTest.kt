package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.user.UserTestMother.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class IsRelevantActivityRegistrationTest {

    private val testee = IsRelevantActivityRegistration()

    private val birthdate = LocalDate.of(2011, 7, 1)
    private val fiscalYear = 2025

    @Test
    fun `should be relevant when child is younger than 14 at start of activity`() {
        // given
        val user = user().birthdate(birthdate).build()
        val registration = activityRegistration().user(user).start(birthdate.plusYears(14).minusDays(1).atTime(10, 0)).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isTrue()
    }

    @Test
    fun `should not be relevant when child turns 14 on the first day of the activity`() {
        // given
        val user = user().birthdate(birthdate).build()
        val registration = activityRegistration().user(user).start(birthdate.plusYears(14).atStartOfDay()).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isFalse()
    }

    @Test
    fun `should be relevant when child turns 14 during the activity`() {
        // given
        val user = user().birthdate(birthdate).build()
        val start = birthdate.plusYears(14).minusDays(2).atTime(10, 0)
        val registration = activityRegistration().user(user).start(start).end(start.plusDays(5)).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isTrue()
    }

    @Test
    fun `should be relevant when child with handicap is younger than 21 at start of activity`() {
        // given
        val user = user().birthdate(birthdate).hasHandicap().build()
        val registration = activityRegistration().user(user).start(birthdate.plusYears(21).minusDays(1).atTime(10, 0)).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isTrue()
    }

    @Test
    fun `should not be relevant when child with handicap turns 21 on the first day of the activity`() {
        // given
        val user = user().birthdate(birthdate).hasHandicap().build()
        val registration = activityRegistration().user(user).start(birthdate.plusYears(21).atStartOfDay()).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isFalse()
    }

    @Test
    fun `should not be relevant when child without handicap is between 14 and 21`() {
        // given
        val user = user().birthdate(birthdate).build()
        val registration = activityRegistration().user(user).start(birthdate.plusYears(18).atStartOfDay()).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isFalse()
    }

    @Test
    fun `should use the age at the activity start when child turns 14 later in the fiscal year`() {
        // given
        val user = user().birthdate(LocalDate.of(2011, 3, 1)).build()
        val registration = activityRegistration().user(user).start(LocalDateTime.of(fiscalYear, 2, 15, 10, 0)).build()

        // when
        val relevant = testee.forYear(fiscalYear, registration)

        // then
        assertThat(relevant).isTrue()
    }
}
