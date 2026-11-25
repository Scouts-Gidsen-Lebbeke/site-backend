package be.sgl.backend.service.role

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.service.user.sync.CreateExternalFunctions
import be.sgl.backend.service.user.sync.EndExternalFunctions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ExternalRoleAssignerTest {

    @Mock
    lateinit var accessApi: AccessApi
    @Mock
    lateinit var createExternalFunctions: CreateExternalFunctions
    @Mock
    lateinit var endExternalFunctions: EndExternalFunctions
    @InjectMocks
    lateinit var testee: ExternalRoleAssigner

    @Nested
    inner class StartRole {

        @Test
        fun `should create the external functions when starting a staff role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = staffRole(branch().build()).externalId("func").backupExternalId("backup").build()
            given(accessApi.assign(user.id!!, role.id!!)).willReturn(true)

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isTrue()
            then(createExternalFunctions).should().execute("ext-1", "func", "backup")
        }

        @Test
        fun `should skip external synchronization when starting a member role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = memberRole(branch().build()).build()
            given(accessApi.assign(user.id!!, role.id!!)).willReturn(true)

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isTrue()
            then(createExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should skip external synchronization when the user already has the role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = adminRole().build()
            given(accessApi.assign(user.id!!, role.id!!)).willReturn(false)

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isFalse()
            then(createExternalFunctions).shouldHaveNoInteractions()
        }

        @Test
        fun `should skip external synchronization when the user has no external id`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).externalId("func").build()
            given(accessApi.assign(user.id!!, role.id!!)).willReturn(true)

            // when
            val result = testee.startRole(user, role)

            // then
            assertThat(result).isTrue()
            then(createExternalFunctions).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class EndRole {

        @Test
        fun `should revoke the role and end the external functions when ending a staff role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = staffRole(branch().build()).externalId("func").backupExternalId("backup").build()

            // when
            testee.endRole(user, role)

            // then
            then(accessApi).should().revoke(user.id!!, role.id!!)
            then(endExternalFunctions).should().execute("ext-1", "func", "backup")
        }

        @Test
        fun `should end the external functions when ending a member role`() {
            // given
            val user = user().externalId("ext-1").build()
            val role = memberRole(branch().build()).externalId("member").build()

            // when
            testee.endRole(user, role)

            // then
            then(endExternalFunctions).should().execute("ext-1", "member", null)
        }

        @Test
        fun `should only revoke the role when the user has no external id`() {
            // given
            val user = user().build()
            val role = staffRole(branch().build()).externalId("func").build()

            // when
            testee.endRole(user, role)

            // then
            then(accessApi).should().revoke(user.id!!, role.id!!)
            then(endExternalFunctions).shouldHaveNoInteractions()
        }
    }
}
