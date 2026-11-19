package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.util.anyValue
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
class FindRelevantRegistrationsTest {

    @Mock
    lateinit var registrationRepository: ActivityRegistrationRepository
    @Mock
    lateinit var isRelevantActivityRegistration: IsRelevantActivityRegistration
    @InjectMocks
    lateinit var testee: FindRelevantRegistrations

    private val year = 2025
    private val beginOfYear = LocalDateTime.of(year, 1, 1, 0, 0, 0, 0)
    private val endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999_999_999)

    @Test
    fun `should return empty list without relevance checks when no paid registrations exist in the calendar year`() {
        // given
        given(registrationRepository.getPaidRegistrationsBetween(beginOfYear, endOfYear)).willReturn(emptyList())

        // when
        val result = testee.forYear(year)

        // then
        assertThat(result).isEmpty()
        then(isRelevantActivityRegistration).shouldHaveNoInteractions()
    }

    @Test
    fun `should only keep relevant registrations when some registrations are not relevant`() {
        // given
        val relevant = activityRegistration().id(1).paid(true).build()
        val irrelevant = activityRegistration().id(2).paid(true).build()
        given(registrationRepository.getPaidRegistrationsBetween(beginOfYear, endOfYear)).willReturn(listOf(relevant, irrelevant))
        given(isRelevantActivityRegistration.forYear(year, relevant)).willReturn(true)
        given(isRelevantActivityRegistration.forYear(year, irrelevant)).willReturn(false)

        // when
        val result = testee.forYear(year)

        // then
        assertThat(result).containsExactly(relevant)
    }

    @Test
    fun `should query the previous calendar year when searching for the previous year`() {
        // given
        val previousYear = LocalDateTime.now().year - 1
        given(registrationRepository.getPaidRegistrationsBetween(anyValue(), anyValue())).willReturn(emptyList())

        // when
        testee.forPreviousYear()

        // then
        then(registrationRepository).should().getPaidRegistrationsBetween(
            LocalDateTime.of(previousYear, 1, 1, 0, 0, 0, 0),
            LocalDateTime.of(previousYear, 12, 31, 23, 59, 59, 999_999_999)
        )
    }
}
