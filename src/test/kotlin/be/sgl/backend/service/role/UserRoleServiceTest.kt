package be.sgl.backend.service.role

import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.repository.role.UserRoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.shared.exception.RoleNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.shared.exception.UserRoleNotFoundException
import be.sgl.backend.shared.security.RoleLevel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserRoleServiceTest {

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
    lateinit var testee: UserRoleService

    private val branch = branch().id(4).build()

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
    inner class GetUserRolesByRole {

        @Test
        fun `should map the user roles of the role when it exists`() {
            // given
            val role = staffRole(branch).id(2).build()
            val userRole = userRole().user(user().username("jdoe").build()).role(role).build()
            given(roleRepository.findById(2)).willReturn(Optional.of(role))
            given(userRoleRepository.findByRole(role)).willReturn(mutableListOf(userRole))

            // when
            val result = testee.getUserRolesByRole(2)

            // then
            assertThat(result).extracting<String?> { it.user.username }.containsExactly("jdoe")
        }

        @Test
        fun `should throw when the role does not exist`() {
            // given
            given(roleRepository.findById(2)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getUserRolesByRole(2) }
                .isInstanceOf(RoleNotFoundException::class.java)
            then(userRoleRepository).shouldHaveNoInteractions()
        }
    }
}
