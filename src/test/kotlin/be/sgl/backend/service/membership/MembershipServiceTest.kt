package be.sgl.backend.service.membership

import be.sgl.backend.shared.alert.AlertCode
import be.sgl.backend.shared.alert.AlertLogger
import be.sgl.backend.dto.Customer
import be.sgl.backend.dto.membership.MembershipDTO
import be.sgl.backend.dto.membership.UserRegistrationDTOTestMother.userRegistration
import be.sgl.backend.entity.PaymentTestMother.membership
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.shared.exception.MembershipNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.mail.MailService
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MembershipServiceTest {

    @Mock
    lateinit var paymentRepository: MembershipRepository
    @Mock
    lateinit var membershipPeriodRepository: MembershipPeriodRepository
    @Mock
    lateinit var branchRepository: BranchRepository
    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var userDataProvider: UserDataProvider
    @Mock
    lateinit var alertLogger: AlertLogger
    @Mock
    lateinit var validateAndCreateMembership: ValidateAndCreateMembership
    @Mock
    lateinit var createCertificateForMembership: CreateCertificateForMembership
    @Mock
    lateinit var checkoutProvider: CheckoutProvider
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @InjectMocks
    lateinit var testee: MembershipService

    private val username = "username"
    private val checkoutUrl = "https://checkout/url"

    @BeforeEach
    fun injectPaymentServiceFields() {
        // the abstract PaymentService autowires these through fields, which @InjectMocks skips after constructor injection
        ReflectionTestUtils.setField(testee, "checkoutProvider", checkoutProvider)
        ReflectionTestUtils.setField(testee, "mailService", mailService)
    }

    @Nested
    inner class Queries {

        @Test
        fun `should throw when listing the memberships of an unknown user`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getAllMembershipsForUser(username) }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessage("User $username not found")
        }

        @Test
        fun `should map all memberships of the user`() {
            // given
            val user = user().username(username).build()
            val membership = membership().user(user).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(paymentRepository.getMembershipsByUser(user)).willReturn(mutableListOf(membership))

            // when
            val result = testee.getAllMembershipsForUser(username)

            // then
            assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(MembershipDTO.of(membership))
        }

        @Test
        fun `should return no current membership when the user has none`() {
            // given
            val user = user().username(username).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(paymentRepository.getCurrentByUser(user)).willReturn(null)

            // when
            val result = testee.getCurrentMembershipForUser(username)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should throw when getting the current membership of an unknown user`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getCurrentMembershipForUser(username) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should return all current memberships when no branch is given`() {
            // given
            val membership = membership().build()
            given(paymentRepository.getCurrent()).willReturn(mutableListOf(membership))

            // when
            val result = testee.getCurrentMembershipsForBranch(null)

            // then
            assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(MembershipDTO.of(membership))
            then(branchRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should return the current memberships of the branch when a branch is given`() {
            // given
            val branch = branch().build()
            val membership = membership().branch(branch).build()
            given(branchRepository.findById(1)).willReturn(Optional.of(branch))
            given(paymentRepository.getCurrentByBranch(branch)).willReturn(mutableListOf(membership))

            // when
            val result = testee.getCurrentMembershipsForBranch(1)

            // then
            assertThat(result).usingRecursiveFieldByFieldElementComparator().containsExactly(MembershipDTO.of(membership))
        }

        @Test
        fun `should throw when the branch does not exist`() {
            // given
            given(branchRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getCurrentMembershipsForBranch(1) }
                .isInstanceOf(BranchNotFoundException::class.java)
        }

        @Test
        fun `should return null when the membership does not exist`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.empty())

            // when
            val result = testee.getMembershipDTOById(1)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class CreateMembershipForExistingUser {

        @Test
        fun `should throw when the user does not exist`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.createMembershipForExistingUser(username) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should throw when the user already has an active membership`() {
            // given
            val user = user().username(username).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(paymentRepository.getCurrentByUser(user)).willReturn(membership().user(user).build())

            // when / then
            assertThatThrownBy { testee.createMembershipForExistingUser(username) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("There is already an active membership for $username!")
            then(validateAndCreateMembership).shouldHaveNoInteractions()
        }

        @Test
        fun `should create a membership in the active period and return the checkout url`() {
            // given
            val user = user().username(username).build()
            val period = membershipPeriod().build()
            val membership = membership().user(user).period(period).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(paymentRepository.getCurrentByUser(user)).willReturn(null)
            given(membershipPeriodRepository.getActivePeriod()).willReturn(period)
            given(validateAndCreateMembership.execute(eqValue(period), eqValue(user), anyValue())).willReturn(membership)
            given(paymentRepository.save(membership)).willReturn(membership)
            given(checkoutProvider.createCheckoutUrl(Customer(user), membership, "memberships", period.id)).willReturn(checkoutUrl)

            // when
            val result = testee.createMembershipForExistingUser(username)

            // then
            assertThat(result).isEqualTo(checkoutUrl)
            then(paymentRepository).should(times(2)).save(membership)
        }
    }

    @Nested
    inner class CreateMembershipForNewUser {

        private val registration = userRegistration().build()

        @Test
        fun `should throw when the registered user already has a login`() {
            // given
            val existing = user().username(username).build()
            given(userRepository.findByNameAndFirstNameAndBirthdate(registration.name!!, registration.firstName!!, registration.birthdate!!))
                .willReturn(existing)

            // when / then
            assertThatThrownBy { testee.createMembershipForNewUser(registration) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This user already exists, contact the organization to retrieve the login!")
            then(alertLogger).shouldHaveNoInteractions()
        }

        @Test
        fun `should raise an alert and throw when the existing user has no membership`() {
            // given
            val existing = user().build()
            given(userRepository.findByNameAndFirstNameAndBirthdate(registration.name!!, registration.firstName!!, registration.birthdate!!))
                .willReturn(existing)
            given(paymentRepository.getCurrentPossiblyUnpaidByUser(existing)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.createMembershipForNewUser(registration) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This user already exists as an old unlinked account, contact the organization for help with creating the account!")
            then(alertLogger).should().alert(eqValue(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP), anyValue<() -> String>())
        }

        @Test
        fun `should raise an alert and throw when the existing user has a paid membership`() {
            // given
            val existing = user().build()
            given(userRepository.findByNameAndFirstNameAndBirthdate(registration.name!!, registration.firstName!!, registration.birthdate!!))
                .willReturn(existing)
            given(paymentRepository.getCurrentPossiblyUnpaidByUser(existing)).willReturn(membership().user(existing).paid(true).build())

            // when / then
            assertThatThrownBy { testee.createMembershipForNewUser(registration) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("This user already exists but was not yet linked to an account, check the e-mail you received upon registration to create an account!")
            then(alertLogger).should().alert(eqValue(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP), anyValue<() -> String>())
        }

        @Test
        fun `should return the existing checkout url when the previous registration is unpaid`() {
            // given
            val existing = user().build()
            val unpaid = membership().user(existing).paid(false).build()
            given(userRepository.findByNameAndFirstNameAndBirthdate(registration.name!!, registration.firstName!!, registration.birthdate!!))
                .willReturn(existing)
            given(paymentRepository.getCurrentPossiblyUnpaidByUser(existing)).willReturn(unpaid)
            given(checkoutProvider.getCheckoutUrl(unpaid)).willReturn(checkoutUrl)

            // when
            val result = testee.createMembershipForNewUser(registration)

            // then
            assertThat(result).isEqualTo(checkoutUrl)
            then(userRepository).should(never()).save(anyValue<User>())
            then(validateAndCreateMembership).shouldHaveNoInteractions()
        }

        @Test
        fun `should save a new user with the registration data and create a membership`() {
            // given
            val registration = userRegistration().hasReduction().build()
            val period = membershipPeriod().build()
            val membership = membership().period(period).build()
            val userCaptor = argumentCaptor<User>()
            given(userRepository.findByNameAndFirstNameAndBirthdate(registration.name!!, registration.firstName!!, registration.birthdate!!))
                .willReturn(null)
            given(membershipPeriodRepository.getActivePeriod()).willReturn(period)
            given(validateAndCreateMembership.execute(eqValue(period), anyValue(), anyValue())).willReturn(membership)
            given(paymentRepository.save(membership)).willReturn(membership)
            given(checkoutProvider.createCheckoutUrl(anyValue(), eqValue(membership), eqValue("memberships"), eqValue(period.id)))
                .willReturn(checkoutUrl)

            // when
            val result = testee.createMembershipForNewUser(registration)

            // then
            assertThat(result).isEqualTo(checkoutUrl)
            then(userRepository).should().save(captureValue(userCaptor))
            val newUser = userCaptor.value
            assertThat(newUser.username).isNull()
            assertThat(newUser.name).isEqualTo(registration.name)
            assertThat(newUser.firstName).isEqualTo(registration.firstName)
            assertThat(newUser.email).isEqualTo(registration.email)
            assertThat(newUser.birthdate).isEqualTo(registration.birthdate)
            assertThat(newUser.mobile).isEqualTo(registration.mobile)
            assertThat(newUser.sex).isEqualTo(registration.sex)
            assertThat(newUser.hasReduction).isTrue()
            assertThat(newUser.addresses).extracting<String> { it.street }.containsExactly(registration.address!!.street)
        }

        @Test
        fun `should save a new user without address when the registration has none`() {
            // given
            val registration = userRegistration().address(null).build()
            val period = membershipPeriod().build()
            val membership = membership().period(period).build()
            val userCaptor = argumentCaptor<User>()
            given(userRepository.findByNameAndFirstNameAndBirthdate(registration.name!!, registration.firstName!!, registration.birthdate!!))
                .willReturn(null)
            given(membershipPeriodRepository.getActivePeriod()).willReturn(period)
            given(validateAndCreateMembership.execute(eqValue(period), anyValue(), anyValue())).willReturn(membership)
            given(paymentRepository.save(membership)).willReturn(membership)
            given(checkoutProvider.createCheckoutUrl(anyValue(), eqValue(membership), eqValue("memberships"), eqValue(period.id)))
                .willReturn(checkoutUrl)

            // when
            testee.createMembershipForNewUser(registration)

            // then
            then(userRepository).should().save(captureValue(userCaptor))
            assertThat(userCaptor.value.addresses).isEmpty()
        }
    }

    @Nested
    inner class HandlePaymentPaid {

        @Test
        fun `should start the synced role of the branch when there is one`() {
            // given
            val membership = membership().user(user().username(username).build()).build()
            val role = memberRole(membership.branch).build()
            given(roleRepository.getRoleToSyncByBranch(membership.branch)).willReturn(role)
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(membership)

            // then
            then(userDataProvider).should().startRole(membership.user, role)
        }

        @Test
        fun `should not start a role when the branch has no synced role`() {
            // given
            val membership = membership().user(user().username(username).build()).build()
            given(roleRepository.getRoleToSyncByBranch(membership.branch)).willReturn(null)
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(membership)

            // then
            then(userDataProvider).shouldHaveNoInteractions()
        }

        @Test
        fun `should send the confirmation mail when the user has a login`() {
            // given
            val membership = membership().user(user().username(username).build()).price(80.0).build()
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(membership)

            // then
            val params = mapOf(
                "member" to membership.user.firstName,
                "price" to membership.price,
                "periodName" to membership.period.toString(),
                "branchName" to membership.branch.name
            )
            then(mailBuilder).should().to(membership.user.email)
            then(mailBuilder).should().subject("Bevestiging inschrijving")
            then(mailBuilder).should().template("membership-confirmation.html", params)
            then(mailBuilder).should().send()
            then(userDataProvider).should(never()).acceptRegistration(anyValue())
        }

        @Test
        fun `should accept the registration and send the new user mail when the user has no login`() {
            // given
            val membership = membership().user(user().build()).build()
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.handlePaymentPaid(membership)

            // then
            then(userDataProvider).should().acceptRegistration(membership.user)
            then(mailBuilder).should().template(eqValue("membership-new-user-confirmation.html"), anyValue())
            then(mailBuilder).should().send()
        }
    }

    @Nested
    inner class HandlePaymentCanceled {

        @Test
        fun `should delete the user when the user has no login`() {
            // given
            val membership = membership().user(user().build()).build()

            // when
            testee.handlePaymentCanceled(membership)

            // then
            then(userRepository).should().delete(membership.user)
        }

        @Test
        fun `should keep the user when the user has a login`() {
            // given
            val membership = membership().user(user().username(username).build()).build()

            // when
            testee.handlePaymentCanceled(membership)

            // then
            then(userRepository).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class Certificate {

        @Test
        fun `should throw when the membership does not exist`() {
            // given
            given(paymentRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getCertificateForMembership(1) }
                .isInstanceOf(MembershipNotFoundException::class.java)
            then(createCertificateForMembership).shouldHaveNoInteractions()
        }

        @Test
        fun `should create the certificate when the membership exists`() {
            // given
            val membership = membership().paid(true).build()
            val certificate = byteArrayOf(1, 2, 3)
            given(paymentRepository.findById(1)).willReturn(Optional.of<Membership>(membership))
            given(createCertificateForMembership.execute(membership)).willReturn(certificate)

            // when
            val result = testee.getCertificateForMembership(1)

            // then
            assertThat(result).isEqualTo(certificate)
        }
    }
}
