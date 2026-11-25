package be.sgl.backend.service.registrable.activity

import be.sgl.backend.dto.registrable.activity.ActivityRegistrationStatus
import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother.activityRestriction
import be.sgl.backend.entity.user.SiblingRelationTestMother.siblingRelation
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.service.registrable.CalculatePriceFromAdditionalData
import be.sgl.backend.shared.util.reducePrice
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ValidateAndCreateActivityRegistrationTest {

    @Mock
    lateinit var checkRegistrationStatusForUser: CheckRegistrationStatusForUser
    @Mock
    lateinit var calculatePriceFromAdditionalData: CalculatePriceFromAdditionalData
    @Mock
    lateinit var siblingRepository: SiblingRepository
    @Mock
    lateinit var registrationRepository: ActivityRegistrationRepository
    @InjectMocks
    lateinit var testee: ValidateAndCreateActivityRegistration

    private val user = user().id(1).username("member").build()

    @Nested
    inner class Validation {

        @Test
        fun `should fail when the user already has a registration`() {
            // given
            val activity = activity().build()
            val restriction = activityRestriction().activity(activity).build()
            given(checkRegistrationStatusForUser.execute(activity, user))
                .willReturn(ActivityRegistrationStatus(currentRegistration = activityRegistration().build()))

            // when / then
            assertThatThrownBy { testee.execute(restriction, user, null) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Registration for user member already exists!")
        }

        @Test
        fun `should fail when the user has no active membership`() {
            // given
            val activity = activity().build()
            val restriction = activityRestriction().activity(activity).build()
            given(checkRegistrationStatusForUser.execute(activity, user))
                .willReturn(ActivityRegistrationStatus(activeMembership = false))

            // when / then
            assertThatThrownBy { testee.execute(restriction, user, null) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User member has no active membership!")
        }

        @Test
        fun `should fail when the chosen restriction is not an open option`() {
            // given
            val activity = activity().name("Kamp").build()
            val restriction = activityRestriction().id(1).activity(activity).build()
            val otherRestriction = activityRestriction().id(2).activity(activity).build()
            given(checkRegistrationStatusForUser.execute(activity, user))
                .willReturn(ActivityRegistrationStatus(openOptions = listOf(otherRestriction), closedOptions = listOf(restriction)))

            // when / then
            assertThatThrownBy { testee.execute(restriction, user, null) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The chosen restriction is not valid (anymore) for Kamp and member!")
        }
    }

    @Nested
    inner class RegistrationCreation {

        @Test
        fun `should create a registration linked to the activity, user, restriction and additional data`() {
            // given
            val activity = activity().build()
            val restriction = activityRestriction().activity(activity).build()
            val additionalData = """{"size":"M"}"""
            givenOpenOption(restriction)

            // when
            val registration = testee.execute(restriction, user, additionalData)

            // then
            assertThat(registration.subscribable).isSameAs(activity)
            assertThat(registration.user).isSameAs(user)
            assertThat(registration.restriction).isSameAs(restriction)
            assertThat(registration.additionalData).isEqualTo(additionalData)
            assertThat(registration.paid).isFalse()
        }

        @Test
        fun `should use the activity dates when the restriction has no alternative dates`() {
            // given
            val activity = activity().start(LocalDateTime.of(2025, 7, 21, 10, 0)).end(LocalDateTime.of(2025, 7, 31, 10, 0)).build()
            val restriction = activityRestriction().activity(activity).build()
            givenOpenOption(restriction)

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.start).isEqualTo(activity.start)
            assertThat(registration.end).isEqualTo(activity.end)
        }

        @Test
        fun `should use the restriction dates when the restriction has alternative dates`() {
            // given
            val activity = activity().start(LocalDateTime.of(2025, 7, 21, 10, 0)).end(LocalDateTime.of(2025, 7, 31, 10, 0)).build()
            val restriction = activityRestriction().activity(activity)
                .alternativeStart(LocalDateTime.of(2025, 7, 25, 10, 0))
                .alternativeEnd(LocalDateTime.of(2025, 7, 30, 10, 0))
                .build()
            givenOpenOption(restriction)

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.start).isEqualTo(restriction.alternativeStart)
            assertThat(registration.end).isEqualTo(restriction.alternativeEnd)
        }
    }

    @Nested
    inner class PriceCalculation {

        @Test
        fun `should charge the activity price when the restriction has no alternative price`() {
            // given
            val activity = activity().price(150.0).build()
            val restriction = activityRestriction().activity(activity).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(emptyList())

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isEqualTo(activity.price)
        }

        @Test
        fun `should charge the alternative price when the restriction has one`() {
            // given
            val activity = activity().price(150.0).build()
            val restriction = activityRestriction().activity(activity).alternativePrice(120.0).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(emptyList())

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isEqualTo(restriction.alternativePrice)
        }

        @Test
        fun `should add the price from additional data to the base price`() {
            // given
            val activity = activity().price(150.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val additionalData = """{"tshirt":"yes"}"""
            val additionalPrice = 12.5
            givenOpenOption(restriction)
            given(calculatePriceFromAdditionalData.execute(activity, additionalData)).willReturn(additionalPrice)
            given(siblingRepository.getByUser(user)).willReturn(emptyList())

            // when
            val registration = testee.execute(restriction, user, additionalData)

            // then
            assertThat(registration.price).isEqualTo(activity.price + additionalPrice)
        }

        @Test
        fun `should reduce only the base price when the user is entitled to reduction`() {
            // given
            val reducedUser = user().username("reduced").hasReduction().build()
            val activity = activity().price(500.0).reductionFactor(3.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val additionalData = """{"tshirt":"yes"}"""
            val additionalPrice = 10.0
            given(checkRegistrationStatusForUser.execute(activity, reducedUser))
                .willReturn(ActivityRegistrationStatus(openOptions = listOf(restriction)))
            given(calculatePriceFromAdditionalData.execute(activity, additionalData)).willReturn(additionalPrice)

            // when
            val registration = testee.execute(restriction, reducedUser, additionalData)

            // then
            assertThat(registration.price).isEqualTo(activity.price.reducePrice(activity.reductionFactor) + additionalPrice)
        }

        @Test
        fun `should not apply the sibling reduction when the user is entitled to reduction`() {
            // given
            val reducedUser = user().username("reduced").hasReduction().build()
            val activity = activity().price(500.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            given(checkRegistrationStatusForUser.execute(activity, reducedUser))
                .willReturn(ActivityRegistrationStatus(openOptions = listOf(restriction)))

            // when
            testee.execute(restriction, reducedUser, null)

            // then
            then(siblingRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should apply sibling discount when a sibling is already registered`() {
            // given
            val activity = activity().price(500.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val relation = siblingRelation().user(user).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(registrationRepository.existsBySubscribableAndUser(activity, relation.sibling)).willReturn(true)

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isEqualTo(activity.price - activity.siblingReduction)
        }

        @Test
        fun `should apply sibling discount on the price including additional costs`() {
            // given
            val activity = activity().price(500.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val relation = siblingRelation().user(user).build()
            val additionalData = """{"tshirt":"yes"}"""
            val additionalPrice = 20.0
            givenOpenOption(restriction)
            given(calculatePriceFromAdditionalData.execute(activity, additionalData)).willReturn(additionalPrice)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(registrationRepository.existsBySubscribableAndUser(activity, relation.sibling)).willReturn(true)

            // when
            val registration = testee.execute(restriction, user, additionalData)

            // then
            assertThat(registration.price).isEqualTo(activity.price + additionalPrice - activity.siblingReduction)
        }

        @Test
        fun `should clamp the price to zero when the sibling discount exceeds the price`() {
            // given
            val activity = activity().price(50.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val relation = siblingRelation().user(user).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(registrationRepository.existsBySubscribableAndUser(activity, relation.sibling)).willReturn(true)

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isZero()
        }

        @Test
        fun `should not apply sibling discount when the sibling is not registered`() {
            // given
            val activity = activity().price(500.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val relation = siblingRelation().user(user).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))
            given(registrationRepository.existsBySubscribableAndUser(activity, relation.sibling)).willReturn(false)

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isEqualTo(activity.price)
        }

        @Test
        fun `should not apply sibling discount when the registered sibling is entitled to reduction`() {
            // given
            val activity = activity().price(500.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val reducedSibling = user().id(2).hasReduction().build()
            val relation = siblingRelation().user(user).sibling(reducedSibling).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation))

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isEqualTo(activity.price)
            then(registrationRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should apply sibling discount only once when multiple siblings are registered`() {
            // given
            val activity = activity().price(500.0).siblingReduction(100.0).build()
            val restriction = activityRestriction().activity(activity).build()
            val relation = siblingRelation().user(user).sibling(user().id(2).build()).build()
            val otherRelation = siblingRelation().user(user).sibling(user().id(3).build()).build()
            givenOpenOption(restriction)
            given(siblingRepository.getByUser(user)).willReturn(listOf(relation, otherRelation))
            given(registrationRepository.existsBySubscribableAndUser(activity, relation.sibling)).willReturn(true)

            // when
            val registration = testee.execute(restriction, user, null)

            // then
            assertThat(registration.price).isEqualTo(activity.price - activity.siblingReduction)
        }
    }

    private fun givenOpenOption(restriction: ActivityRestriction) {
        given(checkRegistrationStatusForUser.execute(restriction.activity, user))
            .willReturn(ActivityRegistrationStatus(openOptions = listOf(restriction)))
    }
}
