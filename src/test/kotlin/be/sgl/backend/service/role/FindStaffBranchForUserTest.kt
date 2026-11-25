package be.sgl.backend.service.role

import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.entity.role.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.UserNotFoundException
import be.sgl.backend.repository.user.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FindStaffBranchForUserTest {

    @Mock
    lateinit var userRepository: UserRepository
    @InjectMocks
    lateinit var testee: FindStaffBranchForUser

    @Test
    fun `should return the first active staff branch when the user has staff roles`() {
        // given
        val hiddenBranch = branch().id(1).hidden().build()
        val activeBranch = branch().id(2).active().build()
        val user = user().username("jdoe").build()
        userRole().user(user).role(memberRole(branch().id(3).build()).id(1).build()).build()
        userRole().user(user).role(staffRole(hiddenBranch).id(2).build()).build()
        userRole().user(user).role(staffRole(activeBranch).id(3).build()).build()
        given(userRepository.findByUsername("jdoe")).willReturn(user)

        // when
        val result = testee.execute("jdoe")

        // then
        assertThat(result).isSameAs(activeBranch)
    }

    @Test
    fun `should return null when the user has no staff role`() {
        // given
        val user = user().username("jdoe").build()
        userRole().user(user).role(adminRole().build()).build()
        given(userRepository.findByUsername("jdoe")).willReturn(user)

        // when
        val result = testee.execute("jdoe")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should return null when the only staff branch is not active`() {
        // given
        val user = user().username("jdoe").build()
        userRole().user(user).role(staffRole(branch().passive().build()).build()).build()
        given(userRepository.findByUsername("jdoe")).willReturn(user)

        // when
        val result = testee.execute("jdoe")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should throw when the user does not exist`() {
        // given
        given(userRepository.findByUsername("jdoe")).willReturn(null)

        // when / then
        assertThatThrownBy { testee.execute("jdoe") }
            .isInstanceOf(UserNotFoundException::class.java)
            .hasMessage("User jdoe not found")
    }
}
