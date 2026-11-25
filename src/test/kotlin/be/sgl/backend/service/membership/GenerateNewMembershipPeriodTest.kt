package be.sgl.backend.service.membership

import be.sgl.backend.shared.config.LocaleConfig.Companion.BE_NL
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.membership.MembershipRestrictionTestMother.membershipRestriction
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.shared.mail.MailService
import be.sgl.backend.shared.util.I18nUtil
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@ExtendWith(MockitoExtension::class)
class GenerateNewMembershipPeriodTest {

    @Mock
    lateinit var membershipPeriodRepository: MembershipPeriodRepository
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @Mock
    lateinit var applicationContext: ApplicationContext
    @InjectMocks
    lateinit var testee: GenerateNewMembershipPeriod

    private val subject = "Nieuwe lidmaatschapsperiode aangemaakt"
    private val previousStart = LocalDate.of(2025, 9, 1)
    private val previousEnd = LocalDate.of(2026, 8, 31)

    @BeforeEach
    fun initI18n() {
        val messageSource = StaticMessageSource()
        messageSource.addMessage("new.membership.period.created", BE_NL, subject)
        given(applicationContext.getBean(MessageSource::class.java)).willReturn(messageSource)
        I18nUtil().setApplicationContext(applicationContext)
    }

    private fun executeAndCaptureSavedPeriod(previous: MembershipPeriod): MembershipPeriod {
        val captor = argumentCaptor<MembershipPeriod>()
        given(membershipPeriodRepository.save(anyValue<MembershipPeriod>())).willAnswer { it.arguments[0] }
        given(mailService.builder()).willReturn(mailBuilder)
        testee.execute(previous)
        then(membershipPeriodRepository).should().save(captureValue(captor))
        return captor.value
    }

    @Test
    fun `should start the day after the previous period and last equally long`() {
        // given
        val previous = membershipPeriod().start(previousStart).end(previousEnd).build()

        // when
        val newPeriod = executeAndCaptureSavedPeriod(previous)

        // then
        assertThat(newPeriod.start).isEqualTo(previousEnd.plusDays(1))
        assertThat(newPeriod.end).isEqualTo(LocalDate.of(2027, 8, 31))
    }

    @Test
    fun `should copy the price settings of the previous period`() {
        // given
        val previous = membershipPeriod().start(previousStart).end(previousEnd).price(120.0).registrationLimit(200)
            .reductionFactor(2.5).siblingReduction(15.0).build()

        // when
        val newPeriod = executeAndCaptureSavedPeriod(previous)

        // then
        assertThat(newPeriod.price).isEqualTo(previous.price)
        assertThat(newPeriod.registrationLimit).isEqualTo(previous.registrationLimit)
        assertThat(newPeriod.reductionFactor).isEqualTo(previous.reductionFactor)
        assertThat(newPeriod.siblingReduction).isEqualTo(previous.siblingReduction)
    }

    @Test
    fun `should copy the restrictions of the previous period to the new period`() {
        // given
        val branch = branch().build()
        val previous = membershipPeriod().start(previousStart).end(previousEnd)
            .restrictions(membershipRestriction().branch(branch).alternativePrice(80.0).registrationLimit(20).build()).build()

        // when
        val newPeriod = executeAndCaptureSavedPeriod(previous)

        // then
        assertThat(newPeriod.restrictions).hasSize(1)
        val restriction = newPeriod.restrictions[0]
        assertThat(restriction.period).isSameAs(newPeriod)
        assertThat(restriction.branch).isSameAs(branch)
        assertThat(restriction.alternativePrice).isEqualTo(80.0)
        assertThat(restriction.registrationLimit).isEqualTo(20)
    }

    @Test
    fun `should move time restrictions along with the new period`() {
        // given
        val lateRegistrationStart = previousStart.plusMonths(4)
        val previous = membershipPeriod().start(previousStart).end(previousEnd)
            .restrictions(membershipRestriction().alternativeStart(lateRegistrationStart).alternativePrice(150.0).build()).build()

        // when
        val newPeriod = executeAndCaptureSavedPeriod(previous)

        // then
        val periodShift = ChronoUnit.DAYS.between(previous.start, newPeriod.start)
        assertThat(newPeriod.restrictions.single().alternativeStart).isEqualTo(lateRegistrationStart.plusDays(periodShift))
    }

    @Test
    fun `should mail the organization about the new period`() {
        // given
        val newPeriod = membershipPeriod().start(LocalDate.of(2026, 9, 1)).end(LocalDate.of(2027, 8, 31)).build()
        given(mailService.builder()).willReturn(mailBuilder)

        // when
        testee.notifyOrganizationAboutNewPeriod(newPeriod)

        // then
        then(mailBuilder).should().toOrganization()
        then(mailBuilder).should().subject(subject)
        then(mailBuilder).should().template("new-membership-period-created.html", mapOf("periodName" to "01/09/2026 - 31/08/2027"))
        then(mailBuilder).should().send()
    }
}
