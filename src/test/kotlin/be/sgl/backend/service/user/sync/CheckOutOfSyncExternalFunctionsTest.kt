package be.sgl.backend.service.user.sync

import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.openapi.model.GroepsadminTestMother.functie
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
    @InjectMocks
    lateinit var testee: CheckOutOfSyncExternalFunctions

    private fun userWithExternalFunctions(vararg functionIds: String): User {
        given(fetchCurrentlyActiveExternalFunctions.execute(EXTERNAL_ID))
            .willReturn(functionIds.map { functie().functie(it).build() }.toMutableList())
        return user().externalId(EXTERNAL_ID).build()
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
        userRole().user(user).role(memberRole(branch().build()).id(1).externalId("member").build()).build()
        userRole().user(user).role(staffRole(branch().build()).id(2).externalId("staff").backupExternalId("staffBackup").build()).build()

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
        userRole().user(user).role(staffRole(branch().build()).build()).build()

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
            userRole().user(user).role(staffRole(branch().build()).externalId("staff").backupExternalId("backup").build()).build()

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
            userRole().user(user).role(staffRole(branch().build()).externalId("staff").build()).build()

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
            userRole().user(user).role(memberRole(branch().build()).externalId("member").build()).build()

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
            userRole().user(user).role(adminRole().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build()).build()

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.isOutOfSync()).isFalse()
        }

        @Test
        fun `should be in sync when the admin has the AVGA function`() {
            // given
            val user = userWithExternalFunctions(AVGA_FUNCTION)
            userRole().user(user).role(adminRole().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build()).build()

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.isOutOfSync()).isFalse()
        }

        @Test
        fun `should assign the backup function when the admin has neither function`() {
            // given
            val user = userWithExternalFunctions()
            userRole().user(user).role(adminRole().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build()).build()

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.functionsToAssign).containsExactly(AVGA_FUNCTION)
        }

        @Test
        fun `should keep AVGA when the admin has both VGA and AVGA`() {
            // given
            val user = userWithExternalFunctions(VGA_FUNCTION, AVGA_FUNCTION)
            userRole().user(user).role(adminRole().externalId(VGA_FUNCTION).backupExternalId(AVGA_FUNCTION).build()).build()

            // when
            val result = testee.execute(user, false)

            // then
            assertThat(result.functionsToDeassign).isEmpty()
        }

        @Test
        fun `should not fail when the admin role has no backup external id`() {
            // given
            val user = userWithExternalFunctions()
            userRole().user(user).role(adminRole().build()).build()

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
    }
}
