package be.sgl.backend.service.user.sync

import be.sgl.backend.dto.user.ExternalMemberTestMother.externalMember
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.openapi.model.Lid
import be.sgl.backend.shared.mail.MailService
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class RemoveAllExternalFunctionsTest {

    @Mock
    lateinit var fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions
    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @InjectMocks
    lateinit var testee: RemoveAllExternalFunctions

    @Test
    fun `should do nothing when the external member has no external id`() {
        // given
        val member = externalMember().externalId(null).build()

        // when
        testee.execute(member)

        // then
        then(fetchCurrentlyActiveExternalFunctions).shouldHaveNoInteractions()
        then(ledenApi).shouldHaveNoInteractions()
        then(mailService).shouldHaveNoInteractions()
    }

    @Test
    fun `should end all active functions and notify the member`() {
        // given
        val member = externalMember().externalId("ext-1").firstName("John").email("john@doe.com").build()
        val captor = argumentCaptor<Lid>()
        given(fetchCurrentlyActiveExternalFunctions.execute("ext-1"))
            .willReturn(mutableListOf(functie().functie("f1").build(), functie().functie("f2").build()))
        given(mailService.builder()).willReturn(mailBuilder)

        // when
        testee.execute(member)

        // then
        then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), captureValue(captor))
        assertThat(captor.value.functies).extracting("functie").containsExactly("f1", "f2")
        assertThat(captor.value.functies).allSatisfy { assertThat(it.einde).isNotNull() }
        assertThat(captor.value.adressen).isNull()
        then(mailBuilder).should().to("john@doe.com")
        then(mailBuilder).should().template("unsubscribe-confirmation.html", mapOf("member" to "John"))
        then(mailBuilder).should().send()
    }

    @Test
    fun `should end all active functions without notification when the member has no email`() {
        // given
        val member = externalMember().externalId("ext-1").email(null).build()
        given(fetchCurrentlyActiveExternalFunctions.execute("ext-1")).willReturn(mutableListOf())

        // when
        testee.execute(member)

        // then
        then(ledenApi).should().patchLid(eqValue("ext-1"), eqValue(true), anyValue())
        then(mailService).shouldHaveNoInteractions()
    }
}
