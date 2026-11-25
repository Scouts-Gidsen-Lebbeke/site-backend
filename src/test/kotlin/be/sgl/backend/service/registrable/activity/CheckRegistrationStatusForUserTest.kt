package be.sgl.backend.service.registrable.activity

import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother.activityRestriction
import be.sgl.backend.entity.user.MedicalRecordTestMother.medicalRecord
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.shared.util.reducePrice
import org.assertj.core.api.Assertions.assertThat
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
class CheckRegistrationStatusForUserTest {

    @Mock
    lateinit var registrationRepository: ActivityRegistrationRepository
    @Mock
    lateinit var medicalRecordRepository: MedicalRecordRepository
    @Mock
    lateinit var getCurrentValidBranchesForUser: GetCurrentValidBranchesForUser
    @InjectMocks
    lateinit var testee: CheckRegistrationStatusForUser

    private val user = user().username("member").build()
    private val branch = branch().id(1).name("Welpen").build()
    private val passiveBranch = branch().id(2).name("Oud-leiding").passive().build()

    @Nested
    inner class Eligibility {

        @Test
        fun `should return the current registration when the user is already registered`() {
            // given
            val activity = activity().build()
            val registration = activityRegistration().activity(activity).user(user).build()
            given(registrationRepository.getByUserAndSubscribable(user, activity)).willReturn(registration)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.currentRegistration).isSameAs(registration)
            then(getCurrentValidBranchesForUser).shouldHaveNoInteractions()
        }

        @Test
        fun `should report no active membership when the user has no valid branch`() {
            // given
            val activity = activity().build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(emptyList())

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.activeMembership).isFalse()
            assertThat(status.openOptions).isEmpty()
        }

        @Test
        fun `should report no options when the activity has no restriction for the user's branches`() {
            // given
            val restriction = activityRestriction().branch(passiveBranch).build()
            val activity = activity().restrictions(restriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.activeMembership).isTrue()
            assertThat(status.openOptions).isEmpty()
            assertThat(status.closedOptions).isEmpty()
        }

        @Test
        fun `should prioritize restrictions of non-passive branches when both apply`() {
            // given
            val activeRestriction = activityRestriction().id(1).branch(branch).build()
            val passiveRestriction = activityRestriction().id(2).branch(passiveBranch).build()
            val activity = activity().restrictions(activeRestriction, passiveRestriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch, passiveBranch))

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.openOptions).containsExactly(activeRestriction)
        }

        @Test
        fun `should offer passive branch restrictions when no non-passive branch restriction applies`() {
            // given
            val passiveRestriction = activityRestriction().id(2).branch(passiveBranch).build()
            val activity = activity().restrictions(passiveRestriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch, passiveBranch))

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.openOptions).containsExactly(passiveRestriction)
        }
    }

    @Nested
    inner class Limits {

        @Test
        fun `should close all options when the global limit is reached`() {
            // given
            val restriction = activityRestriction().branch(branch).build()
            val activity = activity().registrationLimit(10).restrictions(restriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(activity.registrationLimit)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.openOptions).isEmpty()
            assertThat(status.closedOptions).containsExactly(restriction)
        }

        @Test
        fun `should keep options open when the global limit is not reached yet`() {
            // given
            val restriction = activityRestriction().branch(branch).build()
            val activity = activity().registrationLimit(10).restrictions(restriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(registrationRepository.countPaidRegistrationsByActivity(activity)).willReturn(activity.registrationLimit!! - 1)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.openOptions).containsExactly(restriction)
        }

        @Test
        fun `should close only the options whose restriction limit is reached`() {
            // given
            val fullRestriction = activityRestriction().id(1).branch(branch).name("weekend").alternativeLimit(5).build()
            val openRestriction = activityRestriction().id(2).branch(branch).name("week").alternativeLimit(5).build()
            val activity = activity().restrictions(fullRestriction, openRestriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(registrationRepository.countByRestriction_Id(fullRestriction.id!!)).willReturn(fullRestriction.alternativeLimit)
            given(registrationRepository.countByRestriction_Id(openRestriction.id!!)).willReturn(openRestriction.alternativeLimit!! - 1)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.openOptions).containsExactly(openRestriction)
            assertThat(status.closedOptions).containsExactly(fullRestriction)
        }

        @Test
        fun `should only report closed options when every restriction limit is reached`() {
            // given
            val fullRestriction = activityRestriction().branch(branch).alternativeLimit(5).build()
            val activity = activity().restrictions(fullRestriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(registrationRepository.countByRestriction_Id(fullRestriction.id!!)).willReturn(fullRestriction.alternativeLimit)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.openOptions).isEmpty()
            assertThat(status.closedOptions).containsExactly(fullRestriction)
            then(medicalRecordRepository).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class OpenOptions {

        @Test
        fun `should show reduced prices when the user is entitled to reduction`() {
            // given
            val reducedUser = user().username("reduced").hasReduction().build()
            val restriction = activityRestriction().branch(branch).alternativePrice(150.0).build()
            val activity = activity().reductionFactor(3.0).restrictions(restriction).build()
            given(getCurrentValidBranchesForUser.execute(reducedUser)).willReturn(listOf(branch))

            // when
            val status = testee.execute(activity, reducedUser)

            // then
            assertThat(status.openOptions).singleElement()
                .extracting { it.alternativePrice }
                .isEqualTo(150.0.reducePrice(activity.reductionFactor))
        }

        @Test
        fun `should report medicals as not up to date when the user has no medical record`() {
            // given
            val restriction = activityRestriction().branch(branch).build()
            val activity = activity().restrictions(restriction).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(medicalRecordRepository.findMedicalRecordByUser(user)).willReturn(null)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.medicsDate).isNull()
            assertThat(status.medicalsUpToDate).isFalse()
        }

        @Test
        fun `should report medicals as up to date when the medical record was modified within a year`() {
            // given
            val restriction = activityRestriction().branch(branch).build()
            val activity = activity().restrictions(restriction).build()
            val record = medicalRecord().user(user).lastModifiedDate(LocalDateTime.now().minusMonths(6)).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(medicalRecordRepository.findMedicalRecordByUser(user)).willReturn(record)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.medicsDate).isEqualTo(record.lastModifiedDate)
            assertThat(status.medicalsUpToDate).isTrue()
        }

        @Test
        fun `should report medicals as outdated when the medical record is older than a year`() {
            // given
            val restriction = activityRestriction().branch(branch).build()
            val activity = activity().restrictions(restriction).build()
            val record = medicalRecord().user(user).lastModifiedDate(LocalDateTime.now().minusYears(2)).build()
            given(getCurrentValidBranchesForUser.execute(user)).willReturn(listOf(branch))
            given(medicalRecordRepository.findMedicalRecordByUser(user)).willReturn(record)

            // when
            val status = testee.execute(activity, user)

            // then
            assertThat(status.medicsDate).isEqualTo(record.lastModifiedDate)
            assertThat(status.medicalsUpToDate).isFalse()
        }
    }
}
