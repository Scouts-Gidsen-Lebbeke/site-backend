package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormTestMother.declarationForm
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.exception.LocalizedException
import be.sgl.backend.exception.UserNotFoundException
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.MailService
import generated.Verzendingen
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class BelcotaxServiceTest {

    @Mock
    lateinit var findRelevantRegistrations: FindRelevantRegistrations
    @Mock
    lateinit var filterIntoValidFormData: FilterIntoValidFormData
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var findRelevantUserRegistrations: FindRelevantUserRegistrations
    @Mock
    lateinit var dispatchService: DispatchService
    @Mock
    lateinit var formService: FormService
    @Mock
    lateinit var mailService: MailService
    @InjectMocks
    lateinit var testee: BelcotaxService

    private val user1 = user().id(1).build()
    private val user2 = user().id(2).build()
    private val registration1 = activityRegistration().id(1).user(user1).build()
    private val registration2 = activityRegistration().id(2).user(user2).build()
    private val registration3 = activityRegistration().id(3).user(user1).build()

    @Nested
    inner class GetDispatchForPreviousYear {

        @Test
        fun `should create a dispatch with the forms of every user when relevant registrations exist`() {
            // given
            val form1 = declarationForm().user(user1).build()
            val form2 = declarationForm().user(user2).build()
            val dispatch = Verzendingen()
            given(findRelevantRegistrations.forPreviousYear()).willReturn(listOf(registration1, registration2, registration3))
            given(filterIntoValidFormData.execute(user1, listOf(registration1, registration3))).willReturn(listOf(form1))
            given(filterIntoValidFormData.execute(user2, listOf(registration2))).willReturn(listOf(form2))
            given(dispatchService.createDispatch(listOf(form1, form2))).willReturn(dispatch)

            // when
            val result = testee.getDispatchForPreviousYear()

            // then
            assertThat(result).isSameAs(dispatch)
        }

        @Test
        fun `should throw localized exception when no valid forms remain`() {
            // given
            given(findRelevantRegistrations.forPreviousYear()).willReturn(listOf(registration1))
            given(filterIntoValidFormData.execute(user1, listOf(registration1))).willReturn(emptyList())

            // when / then
            assertThatThrownBy { testee.getDispatchForPreviousYear() }
                .isInstanceOf(LocalizedException::class.java)
                .hasMessage("belcotax.service.dispatch.no.activities")
            then(dispatchService).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw localized exception when there are no relevant registrations`() {
            // given
            given(findRelevantRegistrations.forPreviousYear()).willReturn(emptyList())

            // when / then
            assertThatThrownBy { testee.getDispatchForPreviousYear() }
                .isInstanceOf(LocalizedException::class.java)
                .hasMessage("belcotax.service.dispatch.no.activities")
        }
    }

    @Nested
    inner class GetFormsForUserAndPreviousYear {

        private val username = "username"

        @Test
        fun `should throw user not found when username is unknown`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getFormsForUserAndPreviousYear(username) }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessageContaining(username)
        }

        @Test
        fun `should throw localized exception when user has no valid forms`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(user1)
            given(findRelevantUserRegistrations.forPreviousYear(user1)).willReturn(listOf(registration1))
            given(filterIntoValidFormData.execute(user1, listOf(registration1))).willReturn(emptyList())

            // when / then
            assertThatThrownBy { testee.getFormsForUserAndPreviousYear(username) }
                .isInstanceOf(LocalizedException::class.java)
                .hasMessage("belcotax.service.forms.user.no.activities")
        }

        @Test
        fun `should create a pdf per form when user has valid forms`() {
            // given
            val form1 = declarationForm().user(user1).index(0).build()
            val form2 = declarationForm().user(user1).index(1).build()
            val pdf1 = byteArrayOf(1)
            val pdf2 = byteArrayOf(2)
            given(userRepository.findByUsername(username)).willReturn(user1)
            given(findRelevantUserRegistrations.forPreviousYear(user1)).willReturn(listOf(registration1))
            given(filterIntoValidFormData.execute(user1, listOf(registration1))).willReturn(listOf(form1, form2))
            given(formService.createForm(form1)).willReturn(pdf1)
            given(formService.createForm(form2)).willReturn(pdf2)

            // when
            val result = testee.getFormsForUserAndPreviousYear(username)

            // then
            assertThat(result).containsExactly(pdf1, pdf2)
        }
    }

    @Nested
    inner class GetFormsForPreviousYear {

        @Test
        fun `should group the created pdfs per user when several users have forms`() {
            // given
            val form1 = declarationForm().user(user1).index(0).build()
            val form2 = declarationForm().user(user1).index(1).build()
            val form3 = declarationForm().user(user2).build()
            val pdf1 = byteArrayOf(1)
            val pdf2 = byteArrayOf(2)
            val pdf3 = byteArrayOf(3)
            given(findRelevantRegistrations.forPreviousYear()).willReturn(listOf(registration1, registration2))
            given(filterIntoValidFormData.execute(user1, listOf(registration1))).willReturn(listOf(form1, form2))
            given(filterIntoValidFormData.execute(user2, listOf(registration2))).willReturn(listOf(form3))
            given(formService.createForm(form1)).willReturn(pdf1)
            given(formService.createForm(form2)).willReturn(pdf2)
            given(formService.createForm(form3)).willReturn(pdf3)

            // when
            val result = testee.getFormsForPreviousYear()

            // then
            assertThat(result).containsOnlyKeys(user1, user2)
            assertThat(result[user1]).containsExactly(pdf1, pdf2)
            assertThat(result[user2]).containsExactly(pdf3)
        }

        @Test
        fun `should return empty map when there are no relevant registrations`() {
            // given
            given(findRelevantRegistrations.forPreviousYear()).willReturn(emptyList())

            // when
            val result = testee.getFormsForPreviousYear()

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    inner class MailFormsToUser {

        @Mock(answer = Answers.RETURNS_SELF)
        lateinit var mailBuilder: MailService.MailBuilder

        @Test
        fun `should mail every form as pdf attachment to the user for the previous fiscal year`() {
            // given
            val fiscalYear = LocalDateTime.now().year - 1
            val forms = listOf(byteArrayOf(1), byteArrayOf(2))
            given(mailService.builder()).willReturn(mailBuilder)

            // when
            testee.mailFormsToUser(user1, forms)

            // then
            then(mailBuilder).should().to(user1.email)
            then(mailBuilder).should().subject("Fiscaal attest kinderopvang $fiscalYear")
            then(mailBuilder).should().template("declaration-form-confirmation.html", mapOf("member" to user1.firstName, "fiscalYear" to fiscalYear))
            then(mailBuilder).should().addAttachment(forms[0], "form.pdf", "application/pdf")
            then(mailBuilder).should().addAttachment(forms[1], "form.pdf", "application/pdf")
            then(mailBuilder).should().send()
        }
    }
}
