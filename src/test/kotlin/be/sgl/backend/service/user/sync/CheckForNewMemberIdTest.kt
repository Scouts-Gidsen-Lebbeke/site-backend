package be.sgl.backend.service.user.sync

import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.lid
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.mail.MailService
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
class CheckForNewMemberIdTest {

    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var mailService: MailService
    @Mock(answer = Answers.RETURNS_SELF)
    lateinit var mailBuilder: MailService.MailBuilder
    @InjectMocks
    lateinit var testee: CheckForNewMemberId

    @Test
    fun `should return false when the external member has no member id yet`() {
        // given
        val user = user().externalId("ext-1").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().lidnummer(null).build())

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result).isFalse()
        then(userRepository).shouldHaveNoInteractions()
    }

    @Test
    fun `should return false when the member id is already linked`() {
        // given
        val user = user().externalId("ext-1").memberId("123").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().lidnummer("123").build())

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result).isFalse()
        then(userRepository).shouldHaveNoInteractions()
    }

    @Test
    fun `should report a new member id without saving when the save flag is off`() {
        // given
        val user = user().externalId("ext-1").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().lidnummer("123").build())

        // when
        val result = testee.execute(user, false)

        // then
        assertThat(result).isTrue()
        assertThat(user.memberId).isNull()
        then(userRepository).shouldHaveNoInteractions()
        then(mailService).shouldHaveNoInteractions()
    }

    @Test
    fun `should save the new member id and notify the user when the save flag is on`() {
        // given
        val user = user().externalId("ext-1").firstName("John").email("john@doe.com").build()
        given(ledenApi.getLid("ext-1")).willReturn(lid().lidnummer("123").build())
        given(mailService.builder()).willReturn(mailBuilder)

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result).isTrue()
        assertThat(user.memberId).isEqualTo("123")
        then(userRepository).should().save(user)
        then(mailBuilder).should().to("john@doe.com")
        then(mailBuilder).should().template("member-id-confirmation.html", mapOf("member" to "John", "memberId" to "123"))
        then(mailBuilder).should().send()
    }

    @Test
    fun `should return false without calling Groepsadmin when the user is not linked`() {
        // given
        val user = user().build()

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result).isFalse()
        then(ledenApi).shouldHaveNoInteractions()
    }
}
