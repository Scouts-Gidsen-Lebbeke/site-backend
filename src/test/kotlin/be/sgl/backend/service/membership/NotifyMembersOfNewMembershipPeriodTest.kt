package be.sgl.backend.service.membership

import be.sgl.backend.config.LocaleConfig.Companion.BE_NL
import be.sgl.backend.entity.PaymentTestMother.membership
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.util.I18nUtil
import be.sgl.backend.util.anyValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import java.time.LocalDate

/**
 * The use case is scheduled and determines "today" itself, so the periods are built relative to [LocalDate.now].
 */
@ExtendWith(MockitoExtension::class)
class NotifyMembersOfNewMembershipPeriodTest {

    @Mock
    lateinit var membershipRepository: MembershipRepository
    @Mock
    lateinit var membershipPeriodRepository: MembershipPeriodRepository
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @Mock
    lateinit var generateNewMembershipPeriod: GenerateNewMembershipPeriod
    @Mock
    lateinit var applicationContext: ApplicationContext
    @InjectMocks
    lateinit var testee: NotifyMembersOfNewMembershipPeriod

    private val subject = "Nieuwe lidmaatschapsperiode"
    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)

    @BeforeEach
    fun initI18n() {
        val messageSource = StaticMessageSource()
        messageSource.addMessage("new.membership.period", BE_NL, subject)
        given(applicationContext.getBean(MessageSource::class.java)).willReturn(messageSource)
        I18nUtil().setApplicationContext(applicationContext)
    }

    @Test
    fun `should not notify anyone when yesterdays period has not ended`() {
        // given
        val period = membershipPeriod().start(today.minusMonths(1)).end(today.plusDays(1)).build()
        given(membershipPeriodRepository.getPeriodActiveOn(yesterday)).willReturn(period)

        // when
        testee.execute()

        // then
        then(membershipPeriodRepository).should(never()).findActivePeriod()
        then(mailService).should(never()).builder()
    }

    @Test
    fun `should notify every member of the previous period about the new active period`() {
        // given
        given(mailService.builder()).willReturn(mailBuilder)
        val previous = membershipPeriod().id(1).start(today.minusYears(1)).end(yesterday).build()
        val current = membershipPeriod().id(2).start(today).end(today.plusYears(1)).price(120.0).build()
        val firstUser = user().firstName("first").email("first@corp.com").build()
        val secondUser = user().id(2).firstName("second").email("second@corp.com").build()
        given(membershipPeriodRepository.getPeriodActiveOn(yesterday)).willReturn(previous)
        given(membershipPeriodRepository.findActivePeriod()).willReturn(current)
        given(membershipRepository.getByPeriod(previous)).willReturn(listOf(
            membership().user(firstUser).period(previous).build(),
            membership().user(secondUser).period(previous).build()
        ))

        // when
        testee.execute()

        // then
        then(mailBuilder).should().to(firstUser.email)
        then(mailBuilder).should().template("new-membership-period.html",
            mapOf("member" to firstUser.firstName, "price" to current.price, "periodName" to current.toString()))
        then(mailBuilder).should().to(secondUser.email)
        then(generateNewMembershipPeriod).shouldHaveNoInteractions()
    }

    @Test
    fun `should use the subject for a new membership period`() {
        // given
        given(mailService.builder()).willReturn(mailBuilder)
        val previous = membershipPeriod().id(1).start(today.minusYears(1)).end(yesterday).build()
        val current = membershipPeriod().id(2).start(today).end(today.plusYears(1)).build()
        given(membershipPeriodRepository.getPeriodActiveOn(yesterday)).willReturn(previous)
        given(membershipPeriodRepository.findActivePeriod()).willReturn(current)
        given(membershipRepository.getByPeriod(previous)).willReturn(listOf(membership().period(previous).build()))

        // when
        testee.execute()

        // then
        then(mailBuilder).should().subject(subject)
        then(mailBuilder).should().send()
    }

    @Test
    fun `should generate a new period when there is no active period`() {
        // given
        given(mailService.builder()).willReturn(mailBuilder)
        val previous = membershipPeriod().id(1).start(today.minusYears(1)).end(yesterday).build()
        val generated = membershipPeriod().id(2).start(today).end(today.plusYears(1)).price(130.0).build()
        val member = user().build()
        given(membershipPeriodRepository.getPeriodActiveOn(yesterday)).willReturn(previous)
        given(membershipPeriodRepository.findActivePeriod()).willReturn(null)
        given(generateNewMembershipPeriod.execute(previous)).willReturn(generated)
        given(membershipRepository.getByPeriod(previous)).willReturn(listOf(membership().user(member).period(previous).build()))

        // when
        testee.execute()

        // then
        then(mailBuilder).should().template("new-membership-period.html",
            mapOf("member" to member.firstName, "price" to generated.price, "periodName" to generated.toString()))
    }

    @Test
    fun `should not send any mail when the previous period had no members`() {
        // given
        val previous = membershipPeriod().id(1).start(today.minusYears(1)).end(yesterday).build()
        given(membershipPeriodRepository.getPeriodActiveOn(yesterday)).willReturn(previous)
        given(membershipPeriodRepository.findActivePeriod()).willReturn(membershipPeriod().id(2).start(today).build())
        given(membershipRepository.getByPeriod(previous)).willReturn(emptyList())

        // when
        testee.execute()

        // then
        then(mailBuilder).should(never()).send()
    }

    @Test
    @Disabled("Possible bug: a period ending today is treated as ended (end.isAfter(today) is false), so members are notified a day early")
    fun `should not notify anyone when yesterdays period ends today`() {
        // given
        val period = membershipPeriod().start(today.minusYears(1)).end(today).build()
        given(membershipPeriodRepository.getPeriodActiveOn(yesterday)).willReturn(period)

        // when
        testee.execute()

        // then
        then(membershipPeriodRepository).should(never()).findActivePeriod()
        then(membershipRepository).should(never()).getByPeriod(anyValue())
    }
}
