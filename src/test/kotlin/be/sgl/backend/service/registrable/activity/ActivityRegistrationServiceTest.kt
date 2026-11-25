package be.sgl.backend.service.registrable.activity

import be.sgl.backend.dto.Customer
import be.sgl.backend.dto.registrable.activity.ActivityRegistrationStatus
import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother.activityRestriction
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.ActivityNotFoundException
import be.sgl.backend.shared.exception.ActivityRegistrationNotFoundException
import be.sgl.backend.shared.exception.LocalizedException
import be.sgl.backend.shared.exception.RestrictionNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.mail.MailService
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.shared.util.anyValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ActivityRegistrationServiceTest {

    @Mock
    lateinit var paymentRepository: ActivityRegistrationRepository
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var activityRepository: ActivityRepository
    @Mock
    lateinit var restrictionRepository: ActivityRestrictionRepository
    @Mock
    lateinit var checkRegistrationStatusForUser: CheckRegistrationStatusForUser
    @Mock
    lateinit var validateAndCreateActivityRegistration: ValidateAndCreateActivityRegistration
    @Mock
    lateinit var createCertificateForActivityRegistration: CreateCertificateForActivityRegistration
    @Mock
    lateinit var checkoutProvider: CheckoutProvider
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @InjectMocks
    lateinit var testee: ActivityRegistrationService

    private val now = LocalDateTime.now()
    private val user = user().username("member").email("member@corp.com").build()

    @BeforeEach
    fun injectInheritedCollaborators() {
        ReflectionTestUtils.setField(testee, "checkoutProvider", checkoutProvider)
        ReflectionTestUtils.setField(testee, "mailService", mailService)
    }

    private fun openedActivity() = activity().open(now.minusDays(1)).closed(now.plusDays(5))
        .start(now.plusDays(10)).end(now.plusDays(12))

    @Nested
    inner class Lookups {

        @Test
        fun `should fail to list registrations when the activity is not found`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getAllRegistrationsForActivity(1) }
                .isInstanceOf(ActivityNotFoundException::class.java)
        }

        @Test
        fun `should fail to list registrations when the user is not found`() {
            // given
            given(userRepository.findByUsername("unknown")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getAllRegistrationsForUser("unknown") }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessage("User unknown not found")
        }

        @Test
        fun `should return null when the registration is not found`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.empty())

            // when
            val result = testee.getActivityRegistrationDTOById(1)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should fail to get the status when the user is not found`() {
            // given
            given(activityRepository.findById(1)).willReturn(Optional.of(activity().build()))
            given(userRepository.findByUsername("unknown")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getStatusForActivityAndUser(1, "unknown") }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should map the registration status for the activity and user`() {
            // given
            val activity = activity().build()
            val restriction = activityRestriction().activity(activity).name("weekend").build()
            given(activityRepository.findById(1)).willReturn(Optional.of(activity))
            given(userRepository.findByUsername("member")).willReturn(user)
            given(checkRegistrationStatusForUser.execute(activity, user))
                .willReturn(ActivityRegistrationStatus(openOptions = listOf(restriction), medicalsUpToDate = true))

            // when
            val status = testee.getStatusForActivityAndUser(1, "member")

            // then
            assertThat(status.openOptions).singleElement().extracting { it.name }.isEqualTo(restriction.name)
            assertThat(status.medicalsUpToDate).isTrue()
            assertThat(status.currentRegistration).isNull()
        }

        @Test
        fun `should delegate certificate creation for the registration`() {
            // given
            val registration = activityRegistration().build()
            val certificate = byteArrayOf(1, 2, 3)
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(createCertificateForActivityRegistration.execute(registration)).willReturn(certificate)

            // when
            val result = testee.getCertificateForRegistration(1)

            // then
            assertThat(result).isEqualTo(certificate)
        }

        @Test
        fun `should fail to create a certificate when the registration is not found`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getCertificateForRegistration(1) }
                .isInstanceOf(ActivityRegistrationNotFoundException::class.java)
                .hasMessage("Activity registration not found")
        }
    }

    @Nested
    inner class CreatePaymentForActivity {

        @Test
        fun `should fail when the user is not found`() {
            // given
            given(userRepository.findByUsername("member")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.createPaymentForActivity(1, 1, "member", null) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should fail when the restriction is not found`() {
            // given
            given(userRepository.findByUsername("member")).willReturn(user)
            given(restrictionRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.createPaymentForActivity(1, 1, "member", null) }
                .isInstanceOf(RestrictionNotFoundException::class.java)
                .hasMessage("Restriction not found")
        }

        @Test
        fun `should mark a free registration paid and return the confirmation url`() {
            // given
            val activity = openedActivity().id(3).build()
            val restriction = activityRestriction().activity(activity).build()
            val registration = activityRegistration().activity(activity).user(user).restriction(restriction).price(0.0).build()
            val redirectUrl = "https://site/activities/confirmation.html"
            given(userRepository.findByUsername("member")).willReturn(user)
            given(restrictionRepository.findById(1)).willReturn(Optional.of(restriction))
            given(validateAndCreateActivityRegistration.execute(restriction, user, null)).willReturn(registration)
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.createRedirectUrl(registration, "activities", activity.id)).willReturn(redirectUrl)

            // when
            val url = testee.createPaymentForActivity(activity.id!!, 1, "member", null)

            // then
            assertThat(url).isEqualTo(redirectUrl)
            assertThat(registration.paid).isTrue()
            then(paymentRepository).should(times(2)).save(registration)
            then(checkoutProvider).should(never()).createCheckoutUrl(anyValue(), anyValue(), anyValue(), anyValue())
        }

        @Test
        fun `should link a paid registration to the payment provider and return the checkout url`() {
            // given
            val activity = openedActivity().id(3).build()
            val restriction = activityRestriction().activity(activity).build()
            val registration = activityRegistration().activity(activity).user(user).restriction(restriction).price(100.0).build()
            val checkoutUrl = "https://mollie/checkout"
            given(userRepository.findByUsername("member")).willReturn(user)
            given(restrictionRepository.findById(1)).willReturn(Optional.of(restriction))
            given(validateAndCreateActivityRegistration.execute(restriction, user, null)).willReturn(registration)
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.createCheckoutUrl(Customer(user), registration, "activities", activity.id)).willReturn(checkoutUrl)

            // when
            val url = testee.createPaymentForActivity(activity.id!!, 1, "member", null)

            // then
            assertThat(url).isEqualTo(checkoutUrl)
            assertThat(registration.paid).isFalse()
            then(paymentRepository).should(times(2)).save(registration)
        }

        @Test
        fun `should fail when registrations for the activity are closed`() {
            // given
            val closedActivity = activity().open(now.minusDays(10)).closed(now.minusDays(1)).start(now.plusDays(5)).end(now.plusDays(6)).build()
            val restriction = activityRestriction().activity(closedActivity).build()
            given(userRepository.findByUsername("member")).willReturn(user)
            given(restrictionRepository.findById(1)).willReturn(Optional.of(restriction))

            // when / then
            assertThatThrownBy { testee.createPaymentForActivity(1, 1, "member", null) }
                .isInstanceOf(LocalizedException::class.java)
                .hasMessage("registrable.registrations.not.open")
            then(paymentRepository).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class PaymentHooks {

        @Test
        fun `should not send a confirmation when the activity does not require it`() {
            // given
            val registration = activityRegistration().activity(activity().sendConfirmation(false).build()).build()

            // when
            testee.handlePaymentPaid(registration)

            // then
            then(mailService).shouldHaveNoInteractions()
        }

        @Test
        fun `should send a confirmation with registration details when the activity requires it`() {
            // given
            val activity = activity().name("Kamp").sendConfirmation(true).build()
            val restriction = activityRestriction().activity(activity).branch(branch().name("Welpen").build()).name("weekend").build()
            val registration = activityRegistration().activity(activity).user(user).restriction(restriction)
                .price(100.0).additionalData("""{"size":"M"}""").build()
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(registration)

            // then
            then(mailBuilder).should().to(user.email)
            then(mailBuilder).should().subject("Bevestiging inschrijving")
            then(mailBuilder).should().template("activity-confirmation.html", mapOf(
                "member" to user.firstName,
                "price" to registration.price,
                "activityName" to activity.name,
                "branchName" to "Welpen",
                "restrictionName" to "weekend",
                "additionalData" to mapOf("size" to "M")
            ))
            then(mailBuilder).should(never()).cc(anyValue())
            then(mailBuilder).should().send()
        }

        @Test
        fun `should put the communication address in cc when the activity has one`() {
            // given
            val activity = activity().sendConfirmation(true).communicationCC("cc@corp.com").build()
            val registration = activityRegistration().activity(activity).user(user).build()
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(registration)

            // then
            then(mailBuilder).should().cc("cc@corp.com")
            then(mailBuilder).should().send()
        }

        @Test
        fun `should send a cancellation mail with the refunded amount`() {
            // given
            val activity = activity().name("Kamp").communicationCC("cc@corp.com").build()
            val registration = activityRegistration().activity(activity).user(user).price(100.0).build()
            val refundCost = 1.0
            given(mailService.builder()).willReturn(mailBuilder)
            given(checkoutProvider.getRefundCost(registration)).willReturn(refundCost)

            // when
            testee.handlePaymentRefunded(registration)

            // then
            then(mailBuilder).should().subject("Annulatie inschrijving")
            then(mailBuilder).should().template("cancel-activity-confirmation.html", mapOf(
                "member" to user.firstName,
                "price" to registration.price - refundCost,
                "activityName" to activity.name
            ))
            then(mailBuilder).should().cc("cc@corp.com")
            then(mailBuilder).should().send()
        }
    }

    @Nested
    inner class MarkRegistrationAsCompleted {

        @Test
        fun `should fail when the registration is not paid`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(activityRegistration().paid(false).build()))

            // when / then
            assertThatThrownBy { testee.markRegistrationAsCompleted(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Only a paid activity can be marked as completed!")
        }

        @Test
        fun `should do nothing when the registration is already completed`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(activityRegistration().paid().completed(true).build()))

            // when
            testee.markRegistrationAsCompleted(1)

            // then
            then(paymentRepository).should(never()).save(anyValue())
        }

        @Test
        fun `should fail when the activity starts more than one hour from now`() {
            // given
            val registration = activityRegistration().paid().completed(false).start(now.plusHours(2)).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))

            // when / then
            assertThatThrownBy { testee.markRegistrationAsCompleted(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Registrations can only be completed starting one hour before the activity!")
        }

        @Test
        fun `should mark the registration completed without mail when no completion confirmation is required`() {
            // given
            val registration = activityRegistration().paid().completed(false).start(now.plusMinutes(30))
                .activity(activity().sendCompleteConfirmation(false).build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))

            // when
            testee.markRegistrationAsCompleted(1)

            // then
            assertThat(registration.completed).isTrue()
            then(paymentRepository).should().save(registration)
            then(mailService).shouldHaveNoInteractions()
        }

        @Test
        fun `should send a completion mail when the activity requires completion confirmation`() {
            // given
            val activity = activity().name("Kamp").sendCompleteConfirmation(true).communicationCC("cc@corp.com").build()
            val registration = activityRegistration().paid().completed(false).start(now.minusDays(1)).activity(activity).user(user).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.markRegistrationAsCompleted(1)

            // then
            then(mailBuilder).should().to(user.email)
            then(mailBuilder).should().template("activity-completion.html", mapOf("member" to user.firstName, "activityName" to activity.name))
            then(mailBuilder).should().cc("cc@corp.com")
            then(mailBuilder).should().send()
        }
    }

    @Nested
    inner class CancelRegistration {

        @Test
        fun `should fail when the registration is not paid`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(activityRegistration().paid(false).build()))

            // when / then
            assertThatThrownBy { testee.cancelRegistration(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Only a paid activity registration can be cancelled!")
        }

        @Test
        fun `should fail when registrations are no longer open`() {
            // given
            val closedActivity = activity().open(now.minusDays(10)).closed(now.minusDays(1)).start(now.plusDays(5)).end(now.plusDays(6)).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(activityRegistration().paid().activity(closedActivity).build()))

            // when / then
            assertThatThrownBy { testee.cancelRegistration(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Cancellation is only possible when registrations are still open!")
        }

        @Test
        fun `should refund a paid registration and mark it refunded`() {
            // given
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.refundPayment(registration)).willReturn(true)

            // when
            testee.cancelRegistration(1)

            // then
            then(checkoutProvider).should().refundPayment(registration)
            then(paymentRepository).should().save(registration)
            assertThat(registration.paid).isFalse()
            then(mailService).shouldHaveNoInteractions()
        }

        @Test
        fun `should delete the registration and send the cancellation mail when nothing is refunded`() {
            // given
            val registration = activityRegistration().paid().price(1.0).activity(openedActivity().build()).user(user).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(checkoutProvider.refundPayment(registration)).willReturn(false)
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.cancelRegistration(1)

            // then
            then(paymentRepository).should().delete(registration)
            then(paymentRepository).should(never()).save(anyValue())
            then(mailBuilder).should().send()
        }

        @Test
        fun `should delete a free registration and send the cancellation mail`() {
            // given
            val registration = activityRegistration().paid().price(0.0).activity(openedActivity().build()).user(user).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.cancelRegistration(1)

            // then
            then(paymentRepository).should().delete(registration)
            then(checkoutProvider).should(never()).refundPayment(anyValue())
            then(mailBuilder).should().send()
        }

        @Test
        fun `should not mention a negative refund when cancelling a free registration`() {
            // given
            val activity = activity().name("Kamp").open(now.minusDays(1)).closed(now.plusDays(5)).start(now.plusDays(10)).end(now.plusDays(12)).build()
            val registration = activityRegistration().paid().price(0.0).activity(activity).user(user).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(mailService.builder()).willReturn(mailBuilder)
            given(checkoutProvider.getRefundCost(registration)).willReturn(1.0)

            // when
            testee.cancelRegistration(1)

            // then
            then(mailBuilder).should().template("cancel-activity-confirmation.html", mapOf(
                "member" to user.firstName,
                "price" to 0.0,
                "activityName" to activity.name
            ))
        }

        @Test
        fun `should refund a registration of a non cancellable activity when staff cancels it by id`() {
            // given
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().cancellable(false).build()).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.refundPayment(registration)).willReturn(true)

            // when
            testee.cancelRegistration(1)

            // then
            then(checkoutProvider).should().refundPayment(registration)
        }
    }

    @Nested
    inner class CancelRegistrationForUser {

        @Test
        fun `should cancel the registration of the user itself`() {
            // given
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().build()).user(user).build()
            given(userRepository.findByUsername("member")).willReturn(user)
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.refundPayment(registration)).willReturn(true)

            // when
            testee.cancelRegistrationForUser(1, "member")

            // then
            then(checkoutProvider).should().refundPayment(registration)
        }

        @Test
        fun `should hide the registration of another user`() {
            // given
            val owner = user().id(2).username("owner").build()
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().build()).user(owner).build()
            given(userRepository.findByUsername("member")).willReturn(user)
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))

            // when / then
            assertThatThrownBy { testee.cancelRegistrationForUser(1, "member") }
                .isInstanceOf(ActivityRegistrationNotFoundException::class.java)
            then(checkoutProvider).should(never()).refundPayment(anyValue())
        }

        @Test
        fun `should let staff cancel the registration of another user`() {
            // given
            val staff = user().id(3).username("staff").build()
            userRole().user(staff).role(staffRole(branch().build()).build()).build()
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().build()).user(user).build()
            given(userRepository.findByUsername("staff")).willReturn(staff)
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.refundPayment(registration)).willReturn(true)

            // when
            testee.cancelRegistrationForUser(1, "staff")

            // then
            then(checkoutProvider).should().refundPayment(registration)
        }

        @Test
        fun `should fail when the user cancels a registration of a non cancellable activity`() {
            // given
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().cancellable(false).build()).user(user).build()
            given(userRepository.findByUsername("member")).willReturn(user)
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))

            // when / then
            assertThatThrownBy { testee.cancelRegistrationForUser(1, "member") }
                .isInstanceOf(LocalizedException::class.java)
                .hasMessage("registrable.registration.not.cancellable")
            then(checkoutProvider).should(never()).refundPayment(anyValue())
        }

        @Test
        fun `should let staff cancel a registration of a non cancellable activity`() {
            // given
            val staff = user().id(3).username("staff").build()
            userRole().user(staff).role(staffRole(branch().build()).build()).build()
            val registration = activityRegistration().paid().price(100.0).activity(openedActivity().cancellable(false).build()).user(user).build()
            given(userRepository.findByUsername("staff")).willReturn(staff)
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(paymentRepository.save(registration)).willReturn(registration)
            given(checkoutProvider.refundPayment(registration)).willReturn(true)

            // when
            testee.cancelRegistrationForUser(1, "staff")

            // then
            then(checkoutProvider).should().refundPayment(registration)
        }

        @Test
        fun `should fail when the user is unknown`() {
            // given
            given(userRepository.findByUsername("unknown")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.cancelRegistrationForUser(1, "unknown") }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    inner class GetPaymentForRegistration {

        @Test
        fun `should fail when the registration is already paid`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.of(activityRegistration().paid().build()))

            // when / then
            assertThatThrownBy { testee.getPaymentForRegistration(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This activity is already paid!")
        }

        @Test
        fun `should return the existing checkout url when the registration is not paid`() {
            // given
            val registration = activityRegistration().paid(false).build()
            given(paymentRepository.findById(1)).willReturn(Optional.of(registration))
            given(checkoutProvider.getCheckoutUrl(registration)).willReturn("https://mollie/checkout")

            // when
            val url = testee.getPaymentForRegistration(1)

            // then
            assertThat(url).isEqualTo("https://mollie/checkout")
        }
    }
}
