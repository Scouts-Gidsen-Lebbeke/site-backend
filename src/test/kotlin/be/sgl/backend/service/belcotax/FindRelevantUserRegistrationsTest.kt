package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FindRelevantUserRegistrationsTest {

    @Mock
    lateinit var registrationRepository: ActivityRegistrationRepository
    @Mock
    lateinit var isRelevantActivityRegistration: IsRelevantActivityRegistration
    @InjectMocks
    lateinit var testee: FindRelevantUserRegistrations

    private val year = 2025
    private val beginOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0, 0)
    private val endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999_999_999)

    @Test
    fun `should return empty list without relevance checks when user has no paid registrations in the calendar year`() {
        // given
        val user = user().build()
        given(registrationRepository.getPaidRegistrationsForUserBetween(user, beginOfYear, endOfYear)).willReturn(emptyList())

        // when
        val result = testee.forYear(year, user)

        // then
        assertThat(result).isEmpty()
        then(isRelevantActivityRegistration).shouldHaveNoInteractions()
    }

    @Test
    fun `should only keep relevant registrations when some registrations are not relevant`() {
        // given
        val user = user().build()
        val relevant = activityRegistration().id(1).user(user).paid(true).build()
        val irrelevant = activityRegistration().id(2).user(user).paid(true).build()
        given(registrationRepository.getPaidRegistrationsForUserBetween(user, beginOfYear, endOfYear)).willReturn(listOf(relevant, irrelevant))
        given(isRelevantActivityRegistration.forYear(year, relevant)).willReturn(true)
        given(isRelevantActivityRegistration.forYear(year, irrelevant)).willReturn(false)

        // when
        val result = testee.forYear(year, user)

        // then
        assertThat(result).containsExactly(relevant)
    }

    @Test
    fun `should query the previous calendar year when searching for the previous year`() {
        // given
        val user = user().build()
        val previousYear = LocalDateTime.now().year - 1
        given(registrationRepository.getPaidRegistrationsForUserBetween(eqValue(user), anyValue(), anyValue())).willReturn(emptyList())

        // when
        testee.forPreviousYear(user)

        // then
        then(registrationRepository).should().getPaidRegistrationsForUserBetween(
            user,
            LocalDateTime.of(previousYear, 1, 1, 0, 0, 0, 0),
            LocalDateTime.of(previousYear, 12, 31, 23, 59, 59, 999_999_999)
        )
    }
}
