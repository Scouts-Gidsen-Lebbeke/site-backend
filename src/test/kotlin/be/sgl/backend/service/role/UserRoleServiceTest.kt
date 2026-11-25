package be.sgl.backend.service.role

import be.sgl.backend.access.AccessApi
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.organization.application.BranchService
import be.sgl.backend.organization.application.RoleService
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.organization.web.BranchDTO
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.exception.RoleNotFoundException
import be.sgl.backend.shared.exception.UserNotFoundException
import jakarta.persistence.EntityManager
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
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class UserRoleServiceTest {

    @Mock
    lateinit var roleService: RoleService
    @Mock
    lateinit var roleRepository: RoleRepository
    @Mock
    lateinit var branchService: BranchService
    @Mock
    lateinit var roleAssigner: RoleAssigner
    @Mock
    lateinit var accessApi: AccessApi
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var entityManager: EntityManager
    @InjectMocks
    lateinit var testee: UserRoleService

    private val branchId = 4
    private val branch = branch().id(branchId).build()
    private val username = "jdoe"

    @Nested
    inner class AssignRoles {

        @Test
        fun `should start the staff role of the branch when marking a user as staff`() {
            // given
            val user = user().username(username).build()
            val role = staffRole(branch).id(2).build()
            given(roleService.getBranchById(branchId)).willReturn(branch)
            given(roleRepository.getStaffRoleToSyncByBranch(branch)).willReturn(role)
            given(userRepository.findByUsername(username)).willReturn(user)
            given(roleAssigner.startRole(user, role)).willReturn(true)

            // when
            val result = testee.markUserAsStaff(branchId, username)

            // then
            assertThat(result.role.id).isEqualTo(role.id)
            assertThat(result.user.username).isEqualTo(username)
        }

        @Test
        fun `should fail when the branch has no staff role`() {
            // given
            given(roleService.getBranchById(branchId)).willReturn(branch)
            given(roleRepository.getStaffRoleToSyncByBranch(branch)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.markUserAsStaff(branchId, username) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("No staff role configured for the given branch!")
            then(roleAssigner).shouldHaveNoInteractions()
        }

        @Test
        fun `should start the admin role when marking a user as admin`() {
            // given
            val user = user().username(username).build()
            val role = adminRole().id(2).build()
            given(roleService.findAdminRole()).willReturn(role)
            given(userRepository.findByUsername(username)).willReturn(user)
            given(roleAssigner.startRole(user, role)).willReturn(true)

            // when
            val result = testee.markUserAsAdmin(username)

            // then
            assertThat(result.role.id).isEqualTo(role.id)
        }

        @Test
        fun `should refresh the user after starting the role when assigning a role`() {
            // given
            val user = user().username(username).build()
            val role = adminRole().id(2).build()
            given(roleService.findAdminRole()).willReturn(role)
            given(userRepository.findByUsername(username)).willReturn(user)
            given(roleAssigner.startRole(user, role)).willReturn(true)

            // when
            testee.markUserAsAdmin(username)

            // then
            val order = inOrder(roleAssigner, entityManager)
            order.verify(roleAssigner).startRole(user, role)
            order.verify(entityManager).flush()
            order.verify(entityManager).refresh(user)
        }

        @Test
        fun `should fail when the user already has the role`() {
            // given
            val user = user().username(username).build()
            val role = adminRole().id(2).build()
            given(roleService.findAdminRole()).willReturn(role)
            given(userRepository.findByUsername(username)).willReturn(user)
            given(roleAssigner.startRole(user, role)).willReturn(false)

            // when / then
            assertThatThrownBy { testee.markUserAsAdmin(username) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("User already has this role!")
            then(entityManager).shouldHaveNoInteractions()
        }

        @Test
        fun `should fail when the user to assign does not exist`() {
            // given
            given(roleService.findAdminRole()).willReturn(adminRole().build())
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.markUserAsAdmin(username) }
                .isInstanceOf(UserNotFoundException::class.java)
            then(roleAssigner).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class DeassignRoleFromUser {

        @Test
        fun `should end the role of the user when both exist`() {
            // given
            val user = user().username(username).build()
            val role = staffRole(branch).id(2).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(roleService.getRoleById(role.id!!)).willReturn(role)

            // when
            testee.deassignRoleFromUser(role.id!!, username)

            // then
            then(roleAssigner).should().endRole(user, role)
        }

        @Test
        fun `should throw when the user does not exist`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.deassignRoleFromUser(2, username) }
                .isInstanceOf(UserNotFoundException::class.java)
            then(roleAssigner).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class GetUserRolesByRole {

        @Test
        fun `should map the holders of the role when it exists`() {
            // given
            val role = staffRole(branch).id(2).build()
            val user = user().username(username).build()
            given(roleService.getRoleById(role.id!!)).willReturn(role)
            given(accessApi.userIdsWithRole(role.id!!)).willReturn(listOf(user.id!!))
            given(userRepository.findAllById(listOf(user.id!!))).willReturn(listOf(user))

            // when
            val result = testee.getUserRolesByRole(role.id!!)

            // then
            assertThat(result).extracting<String?> { it.user.username }.containsExactly(username)
        }

        @Test
        fun `should throw when the role does not exist`() {
            // given
            given(roleService.getRoleById(2)).willAnswer { throw RoleNotFoundException() }

            // when / then
            assertThatThrownBy { testee.getUserRolesByRole(2) }
                .isInstanceOf(RoleNotFoundException::class.java)
            then(accessApi).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class GetStaffBranch {

        @Test
        fun `should return the staff branch when the user has an active one`() {
            // given
            val user = user().username(username).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(accessApi.staffBranchIdOf(user.id!!)).willReturn(branchId)
            given(branchService.getBranch(branchId)).willReturn(BranchDTO.of(branch))

            // when
            val result = testee.getStaffBranch(username)

            // then
            assertThat(result!!.id).isEqualTo(branchId)
        }

        @Test
        fun `should return null when the user has no active staff branch`() {
            // given
            val user = user().username(username).build()
            given(userRepository.findByUsername(username)).willReturn(user)
            given(accessApi.staffBranchIdOf(user.id!!)).willReturn(null)

            // when
            val result = testee.getStaffBranch(username)

            // then
            assertThat(result).isNull()
            then(branchService).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw when the user does not exist`() {
            // given
            given(userRepository.findByUsername(username)).willReturn(null)

            // when / then
            assertThatThrownBy { testee.getStaffBranch(username) }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessage("User $username not found")
            then(accessApi).shouldHaveNoInteractions()
        }
    }
}
