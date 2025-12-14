package be.sgl.backend.service.activity

import be.sgl.backend.dto.registrable.activity.ActivityRegistrationStatus
import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.registrable.activity.ActivityRestrictionTestMother.activityRestriction
import be.sgl.backend.entity.user.SiblingRelation
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.user.SiblingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ValidateAndCreateActivityRegistrationTest {

    @Mock
    lateinit var checkRegistrationStatusForUser: CheckRegistrationStatusForUser
    @Mock
    lateinit var siblingRepository: SiblingRepository
    @Mock
    lateinit var registrationRepository: ActivityRegistrationRepository
    @InjectMocks
    lateinit var testee: ValidateAndCreateActivityRegistration

    @Test
    fun shouldApplySiblingDiscount() {
        // given
        val activity = activity().price(500.0).siblingReduction(100.0).build()
        val restriction = activityRestriction().activity(activity).build()
        val user = user().build()
        val sibling = user().build()
        `when`(checkRegistrationStatusForUser.execute(activity, user))
            .thenReturn(ActivityRegistrationStatus(openOptions = listOf(restriction), medicalsUpToDate = true))
        `when`(siblingRepository.getByUser(user)).thenReturn(listOf(SiblingRelation(user, sibling)))
        `when`(registrationRepository.existsBySubscribableAndUser(activity, sibling)).thenReturn(true)

        // when
        val registration = testee.execute(restriction, user, null)

        // then
        assertThat(registration.price).isEqualTo(activity.price - activity.siblingReduction)
    }
}