package be.sgl.backend.service.user.sync

import be.sgl.backend.access.AccessApi
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.adminRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.memberRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.staffRoleSummary
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.AVGA_FUNCTION
import be.sgl.backend.service.user.inital.CheckForInitialRunExternal.Companion.VGA_FUNCTION
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CheckOutOfSyncExternalFunctionsTest {

    @Mock
    lateinit var fetchCurrentlyActiveExternalFunctions: FetchCurrentlyActiveExternalFunctions
    @Mock
    lateinit var createExternalFunctions: CreateExternalFunctions
    @Mock
    lateinit var endExternalFunctions: EndExternalFunctions
    @Mock
    lateinit var roleApi: RoleApi
    @Mock
    lateinit var accessApi: AccessApi
    @InjectMocks
    lateinit var testee: CheckOutOfSyncExternalFunctions

    private fun userWithExternalFunctions(vararg functionIds: String): User {
        given(fetchCurrentlyActiveExternalFunctions.execute(EXTERNAL_ID))
            .willReturn(functionIds.map { functie().functie(it).build() }.toMutableList())
        return user().id(USER_ID).externalId(EXTERNAL_ID).build()
    }

    private fun givenRoles(vararg roles: RoleSummary) {
        val roleIds = roles.map { it.id }
        given(accessApi.roleIdsOf(USER_ID)).willReturn(roleIds)
        given(roleApi.findRoles(roleIds)).willReturn(roles.toList())
    }

    @Test
    fun `should be in sync without calling Groepsadmin when the user has no external id`() {
        // given
        val user = user().build()

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result.isOutOfSync()).isFalse()
        then(fetchCurrentlyActiveExternalFunctions).shouldHaveNoInteractions()
    }

    @Test
    fun `should be in sync when every role matches an external function`() {
        // given
        val user = userWithExternalFunctions("member", "staff", "staffBackup")
        givenRoles(memberRoleSummary(BRANCH_ID).id(1).externalId("member").build(), staffRoleSummary(BRANCH_ID).id(2).externalId("staff").backupExternalId("staffBackup").build())

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result.isOutOfSync()).isFalse()
        then(createExternalFunctions).shouldHaveNoInteractions()
        then(endExternalFunctions).shouldHaveNoInteractions()
    }

    @Test
    fun `should ignore roles without external ids`() {
        // given
        val user = userWithExternalFunctions()
        givenRoles(staffRoleSummary(BRANCH_ID).build())

        // when
        val result = testee.execute(user, true)

        // then
        assertThat(result.isOutOfSync()).isFalse()
    }

    @Nested
    inner class WhenOutOfSync {

        @Test
        fun `should assign missing functions and end unknown ones when correcting`() {
            // given
            val user = userWithExternalFunctions("unknown")
            givenRoles(staffRoleSummary(BRANCH_ID).externalId("staff").backupExternalId("backup").build())

            // when
            val result = testee.execute(user, true)

            // then
            assertThat(result.functionsToAssign).containsExactly("staff", "backup")
            assertThat(result.functionsToDeassign).containsExactly("unknown")
            then(createExternalFunctions).should().execute(EXTERNAL_ID, "staff", "backup")
            then(endExternalFunctions).should().execute(EXTERNAL_ID, "unknown")
        }

        @Test
        fun `should only report the differences when not correcting`() {
            // given
            val user = userWithExternalFunctions("unknown")
            givenRoles(staffRoleSummary(BRANCH_ID).externalId("staff").build())

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.isOutOfSync()).isTrue()
            assertThat(result.functionsToAssign).containsExactly("staff")
            assertThat(result.functionsToDeassign).containsExactly("unknown")
            then(createExternalFunctions).shouldHaveNoInteractions()
            then(endExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should only assign when nothing needs to be ended`() {
            // given
            val user = userWithExternalFunctions()
            givenRoles(memberRoleSummary(BRANCH_ID).externalId("member").build())

            // when
            testee.execute(user, true)

            // then
            then(createExternalFunctions).should().execute(EXTERNAL_ID, "member")
            then(endExternalFunctions).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class AdminRole {

        @Test
        fun `should be in sync when the admin has the VGA function`() {
            // given
            val user = userWithExternalFunctions(VGA_FUNCTION)
            givenRoles(adminRoleSummary().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build())

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.isOutOfSync()).isFalse()
        }

        @Test
        fun `should be in sync when the admin has the AVGA function`() {
            // given
            val user = userWithExternalFunctions(AVGA_FUNCTION)
            givenRoles(adminRoleSummary().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build())

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.isOutOfSync()).isFalse()
        }

        @Test
        fun `should assign the backup function when the admin has neither function`() {
            // given
            val user = userWithExternalFunctions()
            givenRoles(adminRoleSummary().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build())

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.functionsToAssign).containsExactly(AVGA_FUNCTION)
        }

        @Test
        fun `should keep AVGA when the admin has both VGA and AVGA`() {
            // given
            val user = userWithExternalFunctions(VGA_FUNCTION, AVGA_FUNCTION)
            givenRoles(adminRoleSummary().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build())

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.functionsToDeassign).isEmpty()
        }

        @Test
        fun `should not fail when the admin role has no backup external id`() {
            // given
            val user = userWithExternalFunctions()
            givenRoles(adminRoleSummary().build())

            // when / then
            assertThatCode { testee.execute(user, false) }.doesNotThrowAnyException()
        }
    }

    @Test
    fun `should report the state as in sync when created without differences`() {
        // given
        val state = CheckOutOfSyncExternalFunctions.OutOfSyncState()

        // when
        val outOfSync = state.isOutOfSync()

        // then
        assertThat(outOfSync).isFalse()
    }

    companion object {
        private const val EXTERNAL_ID = "ext-1"
        private const val USER_ID = 1
        private const val BRANCH_ID = 2
    }
}
