package be.sgl.backend.service.registrable.activity

import be.sgl.backend.dto.registrable.activity.CreateOrUpdateActivityRequestTestMother.activityRequest
import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother.activityRestriction
import be.sgl.backend.shared.exception.ActivityNotFoundException
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.shared.exception.LocalizedException
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argThatValue
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
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ActivityServiceTest {

    @Mock
    lateinit var activityRepository: ActivityRepository
    @Mock
    lateinit var registrationRepository: ActivityRegistrationRepository
    @Mock
    lateinit var restrictionRepository: ActivityRestrictionRepository
    @Mock
    lateinit var registrationService: ActivityRegistrationService
    @InjectMocks
    lateinit var testee: ActivityService

    private val now = LocalDateTime.now()

    private fun notYetOpenActivity() = activity().id(1)
        .open(now.plusDays(1)).closed(now.plusDays(10)).start(now.plusDays(20)).end(now.plusDays(22))

    private fun openedActivity() = activity().id(1)
        .open(now.minusDays(1)).closed(now.plusDays(10)).start(now.plusDays(20)).end(now.plusDays(22))

    private fun givenSaveReturnsArgument() {
        given(activityRepository.save(anyValue<Activity>())).willAnswer { it.getArgument<Activity>(0).also { a -> a.id = a.id ?: 1 } }
    }

    @Nested
    inner class GetAllActivities {

        @Test
        fun `should summarize the paid registrations per activity`() {
            // given
            val activity = activity().build()
            val prices = listOf(100.0, 50.0)
            given(activityRepository.findAllRecentFirst()).willReturn(listOf(activity))
            given(registrationRepository.getPaidRegistrationPricesByActivity(activity)).willReturn(prices)

            // when
            val results = testee.getAllActivities()

            // then
            assertThat(results).singleElement().satisfies({
                assertThat(it.registrationCount).isEqualTo(prices.size)
                assertThat(it.totalPrice).isEqualTo(prices.sum())
            })
        }

        @Test
        fun `should fail when the activity is not found`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getActivityDTOById(1) }
                .isInstanceOf(ActivityNotFoundException::class.java)
                .hasMessage("Activity not found")
        }
    }

    @Nested
    inner class CreateActivity {

        @Test
        fun `should create the restrictions anew and link them to the new activity`() {
            // given
            val request = activityRequest().restrictions(activityRestriction().id(7).build()).build()
            givenSaveReturnsArgument()

            // when
            testee.createActivity(request)

            // then
            then(activityRepository).should().save(argThatValue<Activity> { activity ->
                activity.restrictions.single().let { it.id == null && it.activity === activity }
            })
        }

        @Test
        fun `should fail when the closing date is already in the past`() {
            // given
            val request = activityRequest().open(now.minusDays(10)).closed(now.minusDays(1)).build()

            // when / then
            assertThatThrownBy { testee.createActivity(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("New activities cannot be closed for registrations yet!")
        }

        @Test
        fun `should fail when registrations close before they open`() {
            // given
            val request = activityRequest().open(now.plusDays(5)).closed(now.plusDays(5)).build()

            // when / then
            assertThatThrownBy { testee.createActivity(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The closure of registrations should be after the opening of registrations!")
        }

        @Test
        fun `should fail when the activity starts before registrations close`() {
            // given
            val request = activityRequest().closed(now.plusDays(10)).start(now.plusDays(10)).build()

            // when / then
            assertThatThrownBy { testee.createActivity(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of an activity should be after the closure of registrations!")
        }

        @Test
        fun `should fail when the activity ends before it starts`() {
            // given
            val request = activityRequest().start(now.plusDays(20)).end(now.plusDays(20)).build()

            // when / then
            assertThatThrownBy { testee.createActivity(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of an activity should be before its end date!")
        }

        @Test
        fun `should save the activity with its restrictions linked to it`() {
            // given
            val request = activityRequest().build()
            givenSaveReturnsArgument()
            val captor = argumentCaptor<Activity>()

            // when
            val result = testee.createActivity(request)

            // then
            then(activityRepository).should().save(captureValue(captor))
            val saved = captor.value
            assertThat(saved.restrictions).hasSize(request.restrictions.size)
                .allSatisfy { assertThat(it.activity).isSameAs(saved) }
            assertThat(result.name).isEqualTo(request.name)
        }

        @Test
        fun `should reject invalid restrictions for the same branch`() {
            // given
            val branch = branch().id(5).build()
            val request = activityRequest().restrictions(
                activityRestriction().id(1).branch(branch).name("weekend").build(),
                activityRestriction().id(2).branch(branch).build()
            ).build()

            // when / then
            assertThatThrownBy { testee.createActivity(request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("All restrictions should be named")
        }
    }

    @Nested
    inner class UpdateActivity {

        @Test
        fun `should validate the request before looking up the activity`() {
            // given
            val request = activityRequest().start(now.plusDays(20)).end(now.plusDays(19)).build()

            // when / then
            assertThatThrownBy { testee.updateActivity(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The start date of an activity should be before its end date!")
            then(activityRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should fail when the activity is not found`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateActivity(1, activityRequest().build()) }
                .isInstanceOf(ActivityNotFoundException::class.java)
        }

        @Test
        fun `should fail when the activity is cancelled`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.of(notYetOpenActivity().cancelled(true).build()))

            // when / then
            assertThatThrownBy { testee.updateActivity(1, activityRequest().build()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A cancelled activity cannot be edited anymore!")
        }

        @Test
        fun `should fail when the new closing date has already passed`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.of(openedActivity().build()))
            val request = activityRequest().open(now.minusDays(5)).closed(now.minusDays(1)).build()

            // when / then
            assertThatThrownBy { testee.updateActivity(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("An activity with closed registrations cannot be edited anymore!")
        }

        @Test
        fun `should fail when the activity has started`() {
            // given
            val started = activity().id(1).open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(1)).end(now.plusDays(2)).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(started))
            val request = activityRequest().open(now.minusDays(10)).closed(now.minusDays(5)).build()

            // when / then
            assertThatThrownBy { testee.updateActivity(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A started activity cannot be edited anymore!")
        }

        @Test
        fun `should fail when the activity is completed`() {
            // given
            val completed = activity().id(1).open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(3)).end(now.minusDays(1)).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(completed))
            val request = activityRequest().open(now.minusDays(10)).closed(now.minusDays(5)).build()

            // when / then
            assertThatThrownBy { testee.updateActivity(1, request) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A completed activity cannot be edited anymore!")
        }

        @Test
        fun `should reopen registrations when the new closing date lies in the future`() {
            // given
            val closedActivity = activity().id(1).open(now.minusDays(10)).closed(now.minusDays(1)).start(now.plusDays(20)).end(now.plusDays(22)).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(closedActivity))
            given(registrationRepository.countPaidRegistrationsByActivity(closedActivity)).willReturn(0)
            givenSaveReturnsArgument()
            val request = activityRequest().open(now.minusDays(10)).closed(now.plusDays(5)).restrictions().build()

            // when
            val result = testee.updateActivity(1, request)

            // then
            assertThat(result.closed).isEqualTo(request.closed)
        }

        @Nested
        inner class WhenNotYetOpen {

            @Test
            fun `should update price, form, dates and restrictions`() {
                // given
                val oldRestriction = activityRestriction().id(1).build()
                val activity = notYetOpenActivity().price(100.0).restrictions(oldRestriction).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                givenSaveReturnsArgument()
                val request = activityRequest().name("newName").price(200.0).reductionFactor(2.0).siblingReduction(15.0)
                    .additionalFormRule("count * 2").registrationLimit(30).communicationCC("cc@corp.com").build()

                // when
                val result = testee.updateActivity(1, request)

                // then
                then(restrictionRepository).should().deleteAll(listOf(oldRestriction))
                assertThat(result.name).isEqualTo(request.name)
                assertThat(result.price).isEqualTo(request.price)
                assertThat(result.reductionFactor).isEqualTo(request.reductionFactor)
                assertThat(result.siblingReduction).isEqualTo(request.siblingReduction)
                assertThat(result.additionalFormRule).isEqualTo(request.additionalFormRule)
                assertThat(result.registrationLimit).isEqualTo(request.registrationLimit)
                assertThat(result.communicationCC).isEqualTo(request.communicationCC)
                assertThat(result.open).isEqualTo(request.open)
                assertThat(result.start).isEqualTo(request.start)
                assertThat(result.restrictions).hasSize(request.restrictions.size)
            }

            @Test
            fun `should fail when a cancellable activity is made uncancellable`() {
                // given
                given(activityRepository.findById(1)).willReturn(Optional.of(notYetOpenActivity().cancellable(true).build()))
                val request = activityRequest().cancellable(false).build()

                // when / then
                assertThatThrownBy { testee.updateActivity(1, request) }
                    .isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("A previously cancellable activity cannot be made uncancellable!")
            }

            @Test
            fun `should allow an uncancellable activity to become cancellable`() {
                // given
                given(activityRepository.findById(1)).willReturn(Optional.of(notYetOpenActivity().cancellable(false).build()))
                givenSaveReturnsArgument()
                val request = activityRequest().cancellable(true).build()

                // when
                val result = testee.updateActivity(1, request)

                // then
                assertThat(result.cancellable).isTrue()
            }

            @Test
            fun `should link the new restrictions to the updated activity`() {
                // given
                val activity = notYetOpenActivity().build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                givenSaveReturnsArgument()

                // when
                testee.updateActivity(1, activityRequest().build())

                // then
                assertThat(activity.restrictions).allSatisfy { assertThat(it.activity).isSameAs(activity) }
            }
        }

        @Nested
        inner class WhenRegistrationsOpened {

            @Test
            fun `should not alter the price when registrations are opened`() {
                // given
                val activity = openedActivity().price(100.0).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(0)
                givenSaveReturnsArgument()
                val request = activityRequest().open(activity.open).price(999.0).restrictions().build()

                // when
                val result = testee.updateActivity(1, request)

                // then
                assertThat(result.price).isEqualTo(100.0)
            }

            @Test
            fun `should fail when an existing restriction is removed`() {
                // given
                val existing = activityRestriction().id(7).build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                val request = activityRequest().open(activity.open).restrictions().build()

                // when / then
                assertThatThrownBy { testee.updateActivity(1, request) }
                    .isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("Existing activity restrictions cannot be removed once the activity has started!")
            }

            @Test
            fun `should allow updating an opened activity when the existing restrictions are kept`() {
                // given
                val existing = activityRestriction().id(7).alternativePrice(50.0).build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                givenSaveReturnsArgument()
                val request = activityRequest().open(activity.open).restrictions(existing).build()

                // when / then
                assertThatCode { testee.updateActivity(1, request) }.doesNotThrowAnyException()
            }

            @Test
            fun `should link a new restriction to the opened activity`() {
                // given
                val existing = activityRestriction().id(7).name("weekend").build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                givenSaveReturnsArgument()
                val request = activityRequest().open(activity.open)
                    .restrictions(existing, activityRestriction().id(null).name("week").build()).build()

                // when
                testee.updateActivity(1, request)

                // then
                assertThat(activity.restrictions).hasSize(2).allSatisfy { assertThat(it.activity).isSameAs(activity) }
            }

            @Test
            fun `should keep the existing restriction instance and update its editable fields`() {
                // given
                val existing = activityRestriction().id(7).name("weekend").build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                givenSaveReturnsArgument()
                val renamed = activityRestriction().id(7).name("long weekend").alternativeLimit(20).build()
                val request = activityRequest().open(activity.open).restrictions(renamed).build()

                // when
                testee.updateActivity(1, request)

                // then
                assertThat(activity.restrictions).containsExactly(existing)
                assertThat(existing.name).isEqualTo(renamed.name)
                assertThat(existing.alternativeLimit).isEqualTo(renamed.alternativeLimit)
            }

            @Test
            fun `should fail when a restriction of another activity is used`() {
                // given
                val existing = activityRestriction().id(7).name("weekend").build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                val foreign = activityRestriction().id(8).name("other activity").build()
                val request = activityRequest().open(activity.open).restrictions(existing, foreign).build()

                // when / then
                assertThatThrownBy { testee.updateActivity(1, request) }
                    .isInstanceOf(LocalizedException::class.java)
                    .hasMessage("activity.restriction.foreign")
            }

            @Test
            fun `should fail when the branch of an existing restriction is altered`() {
                // given
                val existing = activityRestriction().id(7).branch(branch().id(1).build()).build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                val moved = activityRestriction().id(7).branch(branch().id(2).build()).build()
                val request = activityRequest().open(activity.open).restrictions(moved).build()

                // when / then
                assertThatThrownBy { testee.updateActivity(1, request) }
                    .isInstanceOf(LocalizedException::class.java)
                    .hasMessage("activity.restriction.branch.altered")
            }

            @Test
            fun `should fail when the limit of a restriction is lowered below its registration count`() {
                // given
                val registrationCount = 10
                val existing = activityRestriction().id(7).alternativeLimit(registrationCount).build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countByRestriction_Id(7)).willReturn(registrationCount)
                val lowered = activityRestriction().id(7).alternativeLimit(registrationCount - 1).build()
                val request = activityRequest().open(activity.open).restrictions(lowered).build()

                // when / then
                assertThatThrownBy { testee.updateActivity(1, request) }
                    .isInstanceOf(LocalizedException::class.java)
                    .hasMessage("activity.restriction.limit.below.count")
            }

            @Test
            fun `should accept a restriction limit equal to its registration count`() {
                // given
                val registrationCount = 10
                val existing = activityRestriction().id(7).alternativeLimit(registrationCount + 5).build()
                val activity = openedActivity().restrictions(existing).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countByRestriction_Id(7)).willReturn(registrationCount)
                givenSaveReturnsArgument()
                val lowered = activityRestriction().id(7).alternativeLimit(registrationCount).build()
                val request = activityRequest().open(activity.open).restrictions(lowered).build()

                // when / then
                assertThatCode { testee.updateActivity(1, request) }.doesNotThrowAnyException()
            }

            @Test
            fun `should fail when the registration limit is lowered below the registration count`() {
                // given
                val activity = openedActivity().build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(10)
                val request = activityRequest().open(activity.open).registrationLimit(5).restrictions().build()

                // when / then
                assertThatThrownBy { testee.updateActivity(1, request) }
                    .isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("The registration limit cannot be lowered below the current registration count!")
            }

            @Test
            fun `should accept a registration limit above the registration count`() {
                // given
                val activity = openedActivity().build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(10)
                givenSaveReturnsArgument()
                val request = activityRequest().open(activity.open).registrationLimit(11).restrictions().build()

                // when
                val result = testee.updateActivity(1, request)

                // then
                assertThat(result.registrationLimit).isEqualTo(request.registrationLimit)
            }

            @Test
            fun `should accept removing the registration limit`() {
                // given
                val activity = openedActivity().registrationLimit(10).build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(10)
                givenSaveReturnsArgument()
                val request = activityRequest().open(activity.open).registrationLimit(null).restrictions().build()

                // when
                val result = testee.updateActivity(1, request)

                // then
                assertThat(result.registrationLimit).isNull()
            }

            @Test
            fun `should accept a registration limit equal to the registration count`() {
                // given
                val activity = openedActivity().build()
                given(activityRepository.findById(1)).willReturn(Optional.of(activity))
                given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(10)
                givenSaveReturnsArgument()
                val request = activityRequest().open(activity.open).registrationLimit(10).restrictions().build()

                // when / then
                assertThatCode { testee.updateActivity(1, request) }.doesNotThrowAnyException()
            }
        }
    }

    @Nested
    inner class CancelActivity {

        @Test
        fun `should fail when the activity is already cancelled`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.of(openedActivity().cancelled(true).build()))

            // when / then
            assertThatThrownBy { testee.cancelActivity(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This activity is already cancelled!")
        }

        @Test
        fun `should fail when the activity has started`() {
            // given
            val started = activity().open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(1)).end(now.plusDays(2)).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(started))

            // when / then
            assertThatThrownBy { testee.cancelActivity(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A started activity cannot be cancelled anymore!")
        }

        @Test
        fun `should fail when the activity is completed`() {
            // given
            val completed = activity().open(now.minusDays(10)).closed(now.minusDays(5)).start(now.minusDays(3)).end(now.minusDays(1)).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(completed))

            // when / then
            assertThatThrownBy { testee.cancelActivity(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("A completed activity cannot be cancelled anymore!")
        }

        @Test
        fun `should refund or remove every paid registration and mark the activity cancelled`() {
            // given
            val activity = openedActivity().build()
            val registration = activityRegistration().id(1).activity(activity).paid().price(100.0).build()
            val freeRegistration = activityRegistration().id(2).activity(activity).paid().price(0.0).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(activity))
            given(registrationRepository.getRegistrationsByActivity(activity)).willReturn(listOf(registration, freeRegistration))

            // when
            testee.cancelActivity(1)

            // then
            then(registrationService).should().refundOrRemove(registration)
            then(registrationService).should().refundOrRemove(freeRegistration)
            then(activityRepository).should().save(activity)
            assertThat(activity.cancelled).isTrue()
        }

        @Test
        fun `should mark the activity cancelled without refunds when there are no registrations`() {
            // given
            val activity = notYetOpenActivity().build()
            given(activityRepository.findById(1)).willReturn(Optional.of(activity))
            given(registrationRepository.getRegistrationsByActivity(activity)).willReturn(emptyList())

            // when
            testee.cancelActivity(1)

            // then
            then(registrationService).shouldHaveNoInteractions()
            assertThat(activity.cancelled).isTrue()
        }

        @Test
        fun `should only refund or remove paid registrations`() {
            // given
            val activity = openedActivity().build()
            val unpaid = activityRegistration().id(1).activity(activity).paid(false).price(100.0).build()
            given(activityRepository.findById(1)).willReturn(Optional.of(activity))
            given(registrationRepository.getRegistrationsByActivity(activity)).willReturn(listOf(unpaid))

            // when
            testee.cancelActivity(1)

            // then
            then(registrationService).shouldHaveNoInteractions()
        }
    }
}
