package be.sgl.backend.organization.application

import be.sgl.backend.organization.RoleDeleting
import be.sgl.backend.organization.web.RoleChangeRequestTestMother.memberRoleChangeRequest
import be.sgl.backend.organization.web.RoleChangeRequestTestMother.staffRoleChangeRequest
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.shared.exception.RoleNotFoundException
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.context.ApplicationEventPublisher
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RoleServiceTest {

    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var branchRepository: BranchRepository
    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher
    @InjectMocks
    lateinit var testee: RoleService

    private val branch = branch().id(4).build()

    @Nested
    inner class CreateRoles {

        @Test
        fun `should save a member role for the branch when creating a member role`() {
            // given
            val request = memberRoleChangeRequest().externalId("ext").backupExternalId("backup").name("Kapoenen").build()
            val captor = argumentCaptor<Role>()
            given(branchRepository.findById(4)).willReturn(Optional.of(branch))
            given(roleRepository.save(captureValue(captor))).willAnswer { (it.arguments[0] as Role).apply { id = 10 } }

            // when
            val result = testee.createMemberRole(4, request)

            // then
            assertThat(captor.value.memberRole).isTrue()
            assertThat(result).extracting("id", "name", "externalId", "backupExternalId", "level")
                .containsExactly(10, "Kapoenen", "ext", "backup", RoleLevel.GUEST)
        }

        @Test
        fun `should throw when creating a member role for an unknown branch`() {
            // given
            given(branchRepository.findById(4)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.createMemberRole(4, memberRoleChangeRequest().build()) }
                .isInstanceOf(BranchNotFoundException::class.java)
                .hasMessage("Branch not found")
        }

        @Test
        fun `should save a staff level role when creating a staff role with staff level`() {
            // given
            val request = staffRoleChangeRequest().staffLevel().build()
            val captor = argumentCaptor<Role>()
            given(branchRepository.findById(4)).willReturn(Optional.of(branch))
            given(roleRepository.save(captureValue(captor))).willAnswer { (it.arguments[0] as Role).apply { id = 10 } }

            // when
            testee.createStaffRole(4, request)

            // then
            assertThat(captor.value.staffRole).isTrue()
            assertThat(captor.value.staffBranch).isSameAs(branch)
            assertThat(captor.value.level).isEqualTo(RoleLevel.STAFF)
        }
    }

    @Nested
    inner class UpdateMemberRole {

        @Test
        fun `should update the name and external ids when the role is a member role`() {
            // given
            val role = memberRole(branch).id(2).build()
            val request = memberRoleChangeRequest().name("new").externalId("newExt").backupExternalId("newBackup").build()
            given(roleRepository.findById(2)).willReturn(Optional.of(role))
            given(roleRepository.save(role)).willReturn(role)

            // when
            testee.updateMemberRole(2, request)

            // then
            assertThat(role).extracting("name", "externalId", "backupExternalId", "level")
                .containsExactly("new", "newExt", "newBackup", RoleLevel.GUEST)
        }

        @Test
        fun `should fail when the role is not a member role`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.of(staffRole(branch).id(2).build()))

            // when / then
            assertThatThrownBy { testee.updateMemberRole(2, memberRoleChangeRequest().build()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The requested role to update is not a member role!")
            then(roleRepository).should(never()).save(anyValue())
        }

        @Test
        fun `should throw when the role does not exist`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateMemberRole(2, memberRoleChangeRequest().build()) }
                .isInstanceOf(RoleNotFoundException::class.java)
        }
    }

    @Nested
    inner class UpdateStaffRole {

        @Test
        fun `should update the fields and grant staff level when requested`() {
            // given
            val role = staffRole(branch).id(2).level(RoleLevel.GUEST).build()
            val request = staffRoleChangeRequest().name("new").externalId("ext").backupExternalId("backup").staffLevel().build()
            given(roleRepository.findById(2)).willReturn(Optional.of(role))
            given(roleRepository.save(role)).willReturn(role)

            // when
            testee.updateStaffRole(2, request)

            // then
            assertThat(role).extracting("name", "externalId", "backupExternalId", "level")
                .containsExactly("new", "ext", "backup", RoleLevel.STAFF)
        }

        @Test
        fun `should keep the name and downgrade to guest level when no name and no staff level are given`() {
            // given
            val role = staffRole(branch).id(2).name("Leiding").build()
            val request = staffRoleChangeRequest().name(null).build()
            given(roleRepository.findById(2)).willReturn(Optional.of(role))
            given(roleRepository.save(role)).willReturn(role)

            // when
            testee.updateStaffRole(2, request)

            // then
            assertThat(role.name).isEqualTo("Leiding")
            assertThat(role.level).isEqualTo(RoleLevel.GUEST)
        }

        @Test
        fun `should fail when the role is an admin role`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.of(adminRole().id(2).build()))

            // when / then
            assertThatThrownBy { testee.updateStaffRole(2, staffRoleChangeRequest().staffLevel().build()) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("The requested role to update is not a staff role!")
        }
    }

    @Nested
    inner class DeleteRole {

        @Test
        fun `should announce the deletion before deleting the role when it is not an admin role`() {
            // given
            val role = staffRole(branch).id(2).build()
            given(roleRepository.findById(2)).willReturn(Optional.of(role))

            // when
            testee.deleteRole(2)

            // then
            val inOrder = inOrder(eventPublisher, roleRepository)
            then(eventPublisher).should(inOrder).publishEvent(RoleDeleting(2))
            then(roleRepository).should(inOrder).delete(role)
        }

        @Test
        fun `should fail when deleting an admin role`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.of(adminRole().id(2).build()))

            // when / then
            assertThatThrownBy { testee.deleteRole(2) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Admin roles can't be deleted!")
            then(eventPublisher).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw when the role to delete does not exist`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.deleteRole(2) }
                .isInstanceOf(RoleNotFoundException::class.java)
            then(eventPublisher).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class FindAdminRole {

        @Test
        fun `should return the first admin role when one is configured`() {
            // given
            val adminRole = adminRole().id(9).build()
            given(roleRepository.findByLevel(RoleLevel.ADMIN)).willReturn(mutableListOf(adminRole))

            // when
            val result = testee.findAdminRole()

            // then
            assertThat(result).isSameAs(adminRole)
        }

        @Test
        fun `should fail when no admin role is configured`() {
            // given
            given(roleRepository.findByLevel(RoleLevel.ADMIN)).willReturn(mutableListOf())

            // when / then
            assertThatThrownBy { testee.findAdminRole() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No admin role configured!")
        }
    }

    @Nested
    inner class RolesToSync {

        @Test
        fun `should return null when the branch has no role to sync`() {
            // given
            given(branchRepository.findById(4)).willReturn(Optional.of(branch))
            given(roleRepository.getRoleToSyncByBranch(branch)).willReturn(null)

            // when
            val result = testee.getRoleToSyncByBranch(4)

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `should map the staff role of the branch when there is one`() {
            // given
            given(branchRepository.findById(4)).willReturn(Optional.of(branch))
            given(roleRepository.getStaffRoleToSyncByBranch(branch)).willReturn(staffRole(branch).id(3).build())

            // when
            val result = testee.getStaffRoleToSyncByBranch(4)

            // then
            assertThat(result!!.id).isEqualTo(3)
        }
    }
}
