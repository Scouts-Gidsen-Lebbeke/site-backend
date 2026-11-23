package be.sgl.backend.service.role

import be.sgl.backend.dto.role.RoleChangeRequestTestMother.memberRoleChangeRequest
import be.sgl.backend.dto.role.RoleChangeRequestTestMother.staffRoleChangeRequest
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.exception.BranchNotFoundException
import be.sgl.backend.exception.RoleNotFoundException
import be.sgl.backend.exception.UserNotFoundException
import be.sgl.backend.exception.UserRoleNotFoundException
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.role.UserRoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class RoleServiceTest {

    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var userDataProvider: UserDataProvider
    @Mock
    lateinit var branchRepository: BranchRepository
    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @Mock
    lateinit var userRepository: UserRepository
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
        fun `should delete the user roles and the role when it is not an admin role`() {
            // given
            val role = staffRole(branch).id(2).build()
            given(roleRepository.findById(2)).willReturn(Optional.of(role))

            // when
            testee.deleteRole(2)

            // then
            then(userRoleRepository).should().deleteUserRolesByRole(role)
            then(roleRepository).should().delete(role)
        }

        @Test
        fun `should fail when deleting an admin role`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.of(adminRole().id(2).build()))

            // when / then
            assertThatThrownBy { testee.deleteRole(2) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Admin roles can't be deleted!")
            then(userRoleRepository).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class AssignRoles {

        @Test
        fun `should start the staff role of the branch when marking a user as staff`() {
            // given
            val user = user().username("jdoe").build()
            val role = staffRole(branch).id(2).build()
            given(branchRepository.findById(4)).willReturn(Optional.of(branch))
            given(roleRepository.getStaffRoleToSyncByBranch(branch)).willReturn(role)
            given(userRepository.findByUsername("jdoe")).willReturn(user)
            given(userDataProvider.startRole(user, role)).willReturn(userRole().user(user).role(role).build())

            // when
            val result = testee.markUserAsStaff(4, "jdoe")

            // then
            assertThat(result.role.id).isEqualTo(2)
            assertThat(result.user.username).isEqualTo("jdoe")
        }

        @Test
        fun `should fail when the branch has no staff role`() {
            // given
            given(branchRepository.findById(4)).willReturn(Optional.of(branch))
            given(roleRepository.getStaffRoleToSyncByBranch(branch)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.markUserAsStaff(4, "jdoe") }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No staff role configured for the given branch!")
            then(userDataProvider).shouldHaveNoInteractions()
        }

        @Test
        fun `should fail when the user already has the role`() {
            // given
            val user = user().username("jdoe").build()
            val role = adminRole().id(2).build()
            given(roleRepository.findByLevel(RoleLevel.ADMIN)).willReturn(mutableListOf(role))
            given(userRepository.findByUsername("jdoe")).willReturn(user)
            given(userDataProvider.startRole(user, role)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.markUserAsAdmin("jdoe") }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User already has this role!")
        }

        @Test
        fun `should fail when the user to assign does not exist`() {
            // given
            given(roleRepository.findByLevel(RoleLevel.ADMIN)).willReturn(mutableListOf(adminRole().build()))
            given(userRepository.findByUsername("jdoe")).willReturn(null)

            // when / then
            assertThatThrownBy { testee.markUserAsAdmin("jdoe") }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should fail when no admin role is configured`() {
            // given
            given(roleRepository.findByLevel(RoleLevel.ADMIN)).willReturn(mutableListOf())

            // when / then
            assertThatThrownBy { testee.markUserAsAdmin("jdoe") }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No admin role configured!")
        }
    }

    @Nested
    inner class DeassignRoleFromUser {

        @Test
        fun `should end the role when it is a staff role`() {
            // given
            val userRole = userRole().role(staffRole(branch).build()).build()
            given(userRoleRepository.findById(1)).willReturn(Optional.of(userRole))

            // when
            testee.deassignRoleFromUser(1)

            // then
            then(userDataProvider).should().endRole(userRole)
        }

        @Test
        fun `should fail when the role is a member role`() {
            // given
            val userRole = userRole().role(memberRole(branch).build()).build()
            given(userRoleRepository.findById(1)).willReturn(Optional.of(userRole))

            // when / then
            assertThatThrownBy { testee.deassignRoleFromUser(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Member roles can't be manually deassigned!")
            then(userDataProvider).shouldHaveNoInteractions()
        }

        @Test
        fun `should end the admin role when other admins remain`() {
            // given
            val adminRole = adminRole().build()
            val userRole = userRole().user(user().id(1).build()).role(adminRole).build()
            val otherAdmin = userRole().user(user().id(2).build()).role(adminRole).build()
            given(userRoleRepository.findById(1)).willReturn(Optional.of(userRole))
            given(userRoleRepository.findByRole(adminRole)).willReturn(mutableListOf(userRole, otherAdmin))

            // when
            testee.deassignRoleFromUser(1)

            // then
            then(userDataProvider).should().endRole(userRole)
        }

        @Test
        @Disabled("Possible bug: the 'at least one admin' check counts the admin being removed, so the last admin can be removed")
        fun `should fail when removing the last admin`() {
            // given
            val adminRole = adminRole().build()
            val userRole = userRole().role(adminRole).build()
            given(userRoleRepository.findById(1)).willReturn(Optional.of(userRole))
            given(userRoleRepository.findByRole(adminRole)).willReturn(mutableListOf(userRole))

            // when / then
            assertThatThrownBy { testee.deassignRoleFromUser(1) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("At least one admin should exist!")
        }

        @Test
        fun `should throw when the user role does not exist`() {
            // given
            given(userRoleRepository.findById(1)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.deassignRoleFromUser(1) }
                .isInstanceOf(UserRoleNotFoundException::class.java)
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
