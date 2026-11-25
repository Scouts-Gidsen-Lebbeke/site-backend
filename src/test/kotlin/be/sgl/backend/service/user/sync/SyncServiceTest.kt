package be.sgl.backend.service.user.sync

import be.sgl.backend.shared.alert.AlertCode
import be.sgl.backend.shared.alert.AlertLogger
import be.sgl.backend.dto.user.ExternalMember
import be.sgl.backend.dto.user.ExternalMemberTestMother.externalMember
import be.sgl.backend.entity.PaymentTestMother.membership
import be.sgl.backend.people.domain.User
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.service.user.sync.CheckOutOfSyncExternalFunctions.OutOfSyncState
import be.sgl.backend.shared.util.I18nUtil
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.eqValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SyncServiceTest {

    @Mock
    lateinit var fetchExternalData: FetchExternalData
    @Mock
    lateinit var createUserForExternalMember: CreateUserForExternalMember
    @Mock
    lateinit var fetchExternalMembersById: FetchExternalMembersById
    @Mock
    lateinit var fetchUsersWithExternalOpenRegistrations: FetchUsersWithExternalOpenRegistrations
    @Mock
    lateinit var membershipRepository: MembershipRepository
    @Mock
    lateinit var checkForNewMemberId: CheckForNewMemberId
    @Mock
    lateinit var acceptExternalMembershipRequest: AcceptExternalMembershipRequest
    @Mock
    lateinit var removeAllExternalFunctions: RemoveAllExternalFunctions
    @Mock
    lateinit var checkOutOfSyncExternalFunctions: CheckOutOfSyncExternalFunctions
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var alertLogger: AlertLogger
    @Mock
    lateinit var sseEmitter: SseEmitter
    @InjectMocks
    lateinit var testee: SyncService

    private fun givenExternalMembers(vararg members: ExternalMember) {
        given(fetchExternalMembersById.execute()).willReturn(members.associateBy { it.externalId!! }.toMutableMap())
    }

    private fun givenCurrentMembers(vararg users: User, openRegistrations: Map<User, String> = emptyMap()) {
        given(fetchUsersWithExternalOpenRegistrations.execute()).willReturn(openRegistrations)
        given(membershipRepository.getCurrent()).willReturn(users.map { membership().user(it).build() }.toMutableList())
    }

    private fun shouldHaveSent(message: String) {
        then(sseEmitter).should().send(message as Any)
    }

    private fun shouldHaveAlerted() {
        then(alertLogger).should().alert(eqValue(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK), anyValue<() -> String>())
    }

    @Nested
    inner class SyncUser {

        @Test
        fun `should fetch the external data of the user`() {
            // given
            val user = user().username("jdoe").build()
            given(userRepository.findByUsername("jdoe")).willReturn(user)

            // when
            testee.syncUser("jdoe")

            // then
            then(fetchExternalData).should().execute(user)
        }

        @Test
        fun `should throw when the user does not exist`() {
            // given
            given(userRepository.findByUsername("jdoe")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.syncUser("jdoe") }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessage("User jdoe not found")
        }
    }

    @Test
    fun `should update known users and create users for unknown external members when syncing users`() {
        // given
        val linked = user().id(1).externalId("e1").build()
        val unlinked = user().id(2).build()
        givenExternalMembers(externalMember().externalId("e1").build(), externalMember().externalId("e2").build())
        given(userRepository.findAll()).willReturn(listOf(linked, unlinked))

        // when
        testee.syncUsers(sseEmitter)

        // then
        then(fetchExternalData).should().execute(linked)
        then(fetchExternalData).should().execute(unlinked)
        then(createUserForExternalMember).should().execute("e2")
        then(createUserForExternalMember).should(never()).execute("e1")
    }

    @Nested
    inner class SyncMembers {

        @Test
        fun `should accept the open registration of a member without account`() {
            // given
            val user = user().build()
            givenExternalMembers()
            givenCurrentMembers(user, openRegistrations = mapOf(user to "r1"))
            given(acceptExternalMembershipRequest.execute(user, "r1")).willReturn(true)

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveSent("sync.members.external.request.accepted")
            then(checkForNewMemberId).shouldHaveNoInteractions()
        }

        @Test
        fun `should report manual intervention when the open registration can't be accepted`() {
            // given
            val user = user().build()
            givenExternalMembers()
            givenCurrentMembers(user, openRegistrations = mapOf(user to "r1"))
            given(acceptExternalMembershipRequest.execute(user, "r1")).willReturn(false)

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveSent("sync.members.external.request.manual")
        }

        @Test
        fun `should alert when a member without account is not linked externally`() {
            // given
            givenExternalMembers()
            givenCurrentMembers(user().build())

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveAlerted()
            then(checkForNewMemberId).shouldHaveNoInteractions()
        }

        @Test
        fun `should not look for a member id when the member without account already has one`() {
            // given
            val user = user().externalId("e1").memberId("123").build()
            givenExternalMembers(externalMember().externalId("e1").build())
            givenCurrentMembers(user)

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveSent("sync.members.member.id.already.linked")
            then(checkForNewMemberId).shouldHaveNoInteractions()
            then(removeAllExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should save a new member id for a member without account`() {
            // given
            val user = user().externalId("e1").build()
            givenExternalMembers()
            givenCurrentMembers(user)
            given(checkForNewMemberId.execute(user, true)).willReturn(true)

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveSent("sync.members.new.member.id")
        }

        @Test
        fun `should report no new member id for a member without account`() {
            // given
            val user = user().externalId("e1").build()
            givenExternalMembers()
            givenCurrentMembers(user)
            given(checkForNewMemberId.execute(user, true)).willReturn(false)

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveSent("sync.members.no.new.member.id")
        }

        @Test
        fun `should alert when a member with account is not linked externally`() {
            // given
            givenExternalMembers()
            givenCurrentMembers(user().username("jdoe").build())

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveAlerted()
            then(checkOutOfSyncExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should correct the external functions of a member with account`() {
            // given
            val user = user().username("jdoe").externalId("e1").build()
            givenExternalMembers(externalMember().externalId("e1").build())
            givenCurrentMembers(user)
            given(checkOutOfSyncExternalFunctions.execute(user, true)).willReturn(OutOfSyncState(listOf("a"), emptyList()))

            // when
            testee.syncMembers(sseEmitter)

            // then
            then(sseEmitter).should().send("sync.members.external.functions.corrected" as Any)
            then(removeAllExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should report okay when the functions of a member with account are in sync`() {
            // given
            val user = user().username("jdoe").externalId("e1").build()
            givenExternalMembers()
            givenCurrentMembers(user)
            given(checkOutOfSyncExternalFunctions.execute(user, true)).willReturn(OutOfSyncState())

            // when
            testee.syncMembers(sseEmitter)

            // then
            shouldHaveSent("sync.members.external.functions.okay")
        }

        @Test
        fun `should remove all functions of external members without active membership`() {
            // given
            val stale = externalMember().externalId("e9").build()
            givenExternalMembers(stale)
            givenCurrentMembers()

            // when
            testee.syncMembers(sseEmitter)

            // then
            then(removeAllExternalFunctions).should().execute(stale)
        }
    }

    @Nested
    inner class GetUnsyncedMembers {

        @Test
        fun `should report every kind of unsynced member`() {
            // given
            val withRegistration = user().id(1).build()
            val withMemberId = user().id(2).externalId("e2").memberId("2").build()
            val withNewMemberId = user().id(3).externalId("e3").build()
            val withoutMemberId = user().id(4).externalId("e4").build()
            val outOfSync = user().id(5).username("u5").externalId("e5").build()
            val inSync = user().id(6).username("u6").externalId("e6").build()
            val unlinked = user().id(7).build()
            val unlinkedWithAccount = user().id(8).username("u8").build()
            val stale = externalMember().externalId("e9").build()
            givenExternalMembers(externalMember().externalId("e2").build(), externalMember().externalId("e5").build(), stale)
            givenCurrentMembers(withRegistration, withMemberId, withNewMemberId, withoutMemberId, outOfSync, inSync, unlinked,
                unlinkedWithAccount, openRegistrations = mapOf(withRegistration to "r1"))
            given(checkForNewMemberId.execute(withNewMemberId, false)).willReturn(true)
            given(checkForNewMemberId.execute(withoutMemberId, false)).willReturn(false)
            given(checkOutOfSyncExternalFunctions.execute(outOfSync, false)).willReturn(OutOfSyncState(listOf("a"), listOf("b")))
            given(checkOutOfSyncExternalFunctions.execute(inSync, false)).willReturn(OutOfSyncState())

            // when
            val result = testee.getUnsyncedMembers()

            // then
            assertThat(result.map { it.userId }).containsExactly(1, 2, 3, 4, 5, null)
            assertThat(result.first().syncState).isEqualTo(HasExternalOpenRegistration("r1"))
            assertThat(result[4].syncState).isEqualTo(HasUnmatchedExternalFunctions(listOf("a"), listOf("b")))
            assertThat(result.map { it.syncState!!.name }).containsExactly(
                "HAS_EXTERNAL_OPEN_REGISTRATION", "HAS_EXTERNAL_MEMBER_ID_BUT_NO_ACCOUNT", "HAS_NEW_EXTERNAL_MEMBER_ID",
                "HAS_NO_EXTERNAL_MEMBER_ID_YET", "HAS_UNMATCHED_EXTERNAL_FUNCTIONS", "HAS_NO_ACTIVE_MEMBERSHIP")
            assertThat(result.last().externalId).isEqualTo("e9")
            then(alertLogger).should(org.mockito.Mockito.times(2))
                .alert(eqValue(AlertCode.ACTIVE_MEMBERSHIP_NO_EXTERNAL_LINK), anyValue<() -> String>())
        }
    }

    @Nested
    inner class SyncSingleMember {

        @Test
        fun `should accept the open registration of the user`() {
            // given
            val user = user().build()
            given(userRepository.findById(1)).willReturn(Optional.of(user))
            given(acceptExternalMembershipRequest.execute(user, "r1")).willReturn(true)

            // when
            val result = testee.syncMemberWithExternalOpenRegistration(1, "r1")

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should save the new member id of the user`() {
            // given
            val user = user().build()
            given(userRepository.findById(1)).willReturn(Optional.of(user))
            given(checkForNewMemberId.execute(user, true)).willReturn(true)

            // when
            val result = testee.synMemberWithNewExternalMemberId(1)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `should correct the external functions of the user`() {
            // given
            val user = user().build()
            given(userRepository.findById(1)).willReturn(Optional.of(user))
            given(checkOutOfSyncExternalFunctions.execute(user, true)).willReturn(OutOfSyncState())

            // when
            testee.syncMemberWithUnmatchedExternalFunctions(1)

            // then
            then(checkOutOfSyncExternalFunctions).should().execute(user, true)
        }

        @Test
        fun `should throw when the user to sync does not exist`() {
            // given
            given(userRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.synMemberWithNewExternalMemberId(1) }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessage("User 1 not found")
        }

        @Test
        fun `should remove all external functions of a member without membership`() {
            // given
            val member = externalMember().build()

            // when
            testee.syncMemberWithNoActiveMembership(member)

            // then
            then(removeAllExternalFunctions).should().execute(member)
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initI18n() {
            // I18nUtil holds a static MessageSource; return the message keys so the sent progress messages are assertable
            val messageSource = StaticMessageSource().apply { setUseCodeAsDefaultMessage(true) }
            val context = GenericApplicationContext()
            context.beanFactory.registerSingleton("messageSource", messageSource)
            context.refresh()
            I18nUtil().setApplicationContext(context)
        }
    }
}
