package be.sgl.backend.service.membership

import be.sgl.backend.organization.web.BranchBaseDTO
import be.sgl.backend.dto.membership.CreateOrUpdateMembershipPeriodRequestTestMother.membershipPeriodRequest
import be.sgl.backend.dto.membership.MembershipPeriodResult
import be.sgl.backend.dto.membership.MembershipRestrictionDTO
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.membership.MembershipRestrictionTestMother.membershipRestriction
import be.sgl.backend.shared.exception.MembershipPeriodNotFoundException
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MembershipPeriodServiceTest {

    @Mock
    lateinit var periodRepository: MembershipPeriodRepository
    @Mock
    lateinit var membershipRepository: MembershipRepository
    @InjectMocks
    lateinit var testee: MembershipPeriodService

    private val today = LocalDate.now()
    private val branchDto = BranchBaseDTO.of(branch().build())

    @Test
    fun `should count and sum the paid memberships of every period`() {
        // given
        val period = membershipPeriod().build()
        val paidPrices = listOf(100.0, 50.0)
        given(periodRepository.findAllRecentFirst()).willReturn(listOf(period))
        given(membershipRepository.getPaidByPeriod(period)).willReturn(paidPrices)

        // when
        val result = testee.getAllMembershipPeriods()

        // then
        assertThat(result).containsExactly(MembershipPeriodResult(period.id, period.start, period.end, paidPrices.size, paidPrices.sum()))
    }

    @Test
    fun `should throw when the period to get does not exist`() {
        // given
        given(periodRepository.findById(1)).willReturn(Optional.empty())

        // when / then
        assertThatThrownBy { testee.getMembershipPeriodDTOById(1) }
            .isInstanceOf(MembershipPeriodNotFoundException::class.java)
            .hasMessage("Membership period not found")
    }

    @Nested
    inner class CreatePeriod {

        @Test
        fun `should save the period with its restrictions linked to it`() {
            // given
            val restriction = MembershipRestrictionDTO(null, branchDto, null, 80.0, null)
            val request = membershipPeriodRequest().restrictions(restriction).build()
            given(periodRepository.save(anyValue<MembershipPeriod>())).willReturn(membershipPeriod().build())
            val captor = argumentCaptor<MembershipPeriod>()

            // when
            testee.createPeriod(request)

            // then
            then(periodRepository).should().save(captureValue(captor))
            val saved = captor.value
            assertThat(saved.start).isEqualTo(request.start)
            assertThat(saved.end).isEqualTo(request.end)
            assertThat(saved.price).isEqualTo(request.price)
            assertThat(saved.restrictions).hasSize(1)
            assertThat(saved.restrictions[0].period).isSameAs(saved)
            assertThat(saved.restrictions[0].alternativePrice).isEqualTo(restriction.alternativePrice)
        }

        @Test
        fun `should not save the period when a restriction is invalid`() {
            // given
            val request = membershipPeriodRequest().restrictions(MembershipRestrictionDTO(null, branchDto, null, null, null)).build()

            // when / then
            assertThatThrownBy { testee.createPeriod(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("If a restriction is not time related, it should have either an alternative price or a limit to be useful!")
            then(periodRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should save the period when it has a valid time restriction`() {
            // given
            val request = membershipPeriodRequest().start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2026, 8, 31))
                .restrictions(MembershipRestrictionDTO(null, null, LocalDate.of(2025, 10, 1), 120.0, null)).build()
            given(periodRepository.save(anyValue<MembershipPeriod>())).willReturn(membershipPeriod().build())

            // when / then
            assertThatCode { testee.createPeriod(request) }.doesNotThrowAnyException()
        }

        @Test
        fun `should reject the period when a branch has two non time restrictions in the request`() {
            // given
            val request = membershipPeriodRequest().restrictions(
                MembershipRestrictionDTO(null, branchDto, null, 80.0, null),
                MembershipRestrictionDTO(null, branchDto, null, null, 20)
            ).build()

            // when / then
            assertThatThrownBy { testee.createPeriod(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A branch should at most have one single non-time related restriction!")
        }
    }

    @Nested
    inner class ValidateRestrictions {

        @Test
        fun `should reject two non time restrictions for the same branch`() {
            // given
            val branch = branch().build()
            val restrictions = listOf(
                membershipRestriction().branch(branch).alternativePrice(80.0).build(),
                membershipRestriction().branch(branch).registrationLimit(20).build()
            )

            // when / then
            assertThatThrownBy { testee.validateRestrictions(restrictions) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A branch should at most have one single non-time related restriction!")
        }

        @Test
        fun `should accept a non time and a time restriction for the same branch`() {
            // given
            val branch = branch().build()
            val period = membershipPeriod().start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2026, 8, 31)).build()
            val restrictions = listOf(
                membershipRestriction().period(period).branch(branch).alternativePrice(80.0).build(),
                membershipRestriction().period(period).branch(branch).alternativeStart(LocalDate.of(2025, 10, 1)).alternativePrice(90.0).build()
            )

            // when / then
            assertThatCode { testee.validateRestrictions(restrictions) }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept one non time restriction for each of two branches`() {
            // given
            val restrictions = listOf(
                membershipRestriction().branch(branch().build()).alternativePrice(80.0).build(),
                membershipRestriction().branch(branch().id(2).build()).alternativePrice(80.0).build()
            )

            // when / then
            assertThatCode { testee.validateRestrictions(restrictions) }.doesNotThrowAnyException()
        }

        @Test
        fun `should accept an empty list of restrictions`() {
            // given

            // when / then
            assertThatCode { testee.validateRestrictions(emptyList()) }.doesNotThrowAnyException()
        }
    }

    @Nested
    inner class UpdatePeriod {

        private val id = 1

        @Test
        fun `should throw when the period to update does not exist`() {
            // given
            given(periodRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updatePeriod(id, membershipPeriodRequest().build()) }
                .isInstanceOf(MembershipPeriodNotFoundException::class.java)
        }

        @Test
        fun `should update every field when the period has not started yet`() {
            // given
            val period = membershipPeriod().start(today.plusDays(1)).end(today.plusYears(1)).build()
            val request = membershipPeriodRequest().start(today.plusDays(2)).end(today.plusYears(1).plusDays(2))
                .price(150.0).registrationLimit(99).reductionFactor(2.0).siblingReduction(25.0).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.save(period)).willReturn(period)

            // when
            testee.updatePeriod(id, request)

            // then
            assertThat(period.start).isEqualTo(request.start)
            assertThat(period.end).isEqualTo(request.end)
            assertThat(period.price).isEqualTo(request.price)
            assertThat(period.registrationLimit).isEqualTo(request.registrationLimit)
            assertThat(period.reductionFactor).isEqualTo(request.reductionFactor)
            assertThat(period.siblingReduction).isEqualTo(request.siblingReduction)
        }

        @Test
        fun `should only update the registration limit when the period starts today`() {
            // given
            val originalPrice = 100.0
            val period = membershipPeriod().start(today).end(today.plusYears(1)).price(originalPrice).build()
            val request = membershipPeriodRequest().start(today.plusDays(2)).end(today.plusYears(2)).price(150.0).registrationLimit(99).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.save(period)).willReturn(period)

            // when
            testee.updatePeriod(id, request)

            // then
            assertThat(period.registrationLimit).isEqualTo(request.registrationLimit)
            assertThat(period.start).isEqualTo(today)
            assertThat(period.price).isEqualTo(originalPrice)
        }

        @Test
        fun `should not validate the restrictions when the period has already started`() {
            // given
            val period = membershipPeriod().start(today.minusDays(1)).end(today.plusYears(1)).build()
            val invalidRestriction = MembershipRestrictionDTO(null, branchDto, null, null, null)
            val request = membershipPeriodRequest().restrictions(invalidRestriction).registrationLimit(null).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.save(period)).willReturn(period)

            // when
            testee.updatePeriod(id, request)

            // then
            assertThat(period.registrationLimit).isNull()
        }

        @Test
        fun `should reject invalid restrictions when the period has not started yet`() {
            // given
            val period = membershipPeriod().start(today.plusDays(1)).end(today.plusYears(1)).build()
            val request = membershipPeriodRequest().restrictions(MembershipRestrictionDTO(null, branchDto, null, null, null)).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))

            // when / then
            assertThatThrownBy { testee.updatePeriod(id, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("If a restriction is not time related, it should have either an alternative price or a limit to be useful!")
        }

        @Test
        fun `should replace the restrictions when the period has not started yet`() {
            // given
            val period = membershipPeriod().start(today.plusDays(1)).end(today.plusYears(1)).build()
            val newPrice = 80.0
            val request = membershipPeriodRequest().start(today.plusDays(1)).end(today.plusYears(1))
                .restrictions(MembershipRestrictionDTO(null, branchDto, null, newPrice, null)).build()
            given(periodRepository.findById(id)).willReturn(Optional.of(period))
            given(periodRepository.save(period)).willReturn(period)

            // when
            testee.updatePeriod(id, request)

            // then
            assertThat(period.restrictions).extracting<Double?> { it.alternativePrice }.containsExactly(newPrice)
        }
    }
}
