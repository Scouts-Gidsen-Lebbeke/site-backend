package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRole
import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummaryTestMother.adminRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.memberRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.staffRoleSummary
import be.sgl.backend.shared.exception.UserRoleNotFoundException
import be.sgl.backend.shared.util.anyValue
import org.assertj.core.api.Assertions.assertThatThrownBy
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
class RevokeRoleTest {

    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @Mock
    lateinit var roleApi: RoleApi
    @InjectMocks
    lateinit var testee: RevokeRole

    private val userId = 3
    private val otherUserId = 4
    private val roleId = 5
    private val branchId = 7
    private val assignment = userRole().userId(userId).roleId(roleId).build()

    @Test
    fun `should delete assignment when revoking a staff role`() {
        // given
        given(userRoleRepository.findById(UserRole.Key(userId, roleId))).willReturn(Optional.of(assignment))
        given(roleApi.findRoles(listOf(roleId))).willReturn(listOf(staffRoleSummary(branchId).id(roleId).build()))

        // when
        testee.execute(userId, roleId)

        // then
        then(userRoleRepository).should().delete(assignment)
    }

    @Test
    fun `should refuse when revoking a member role`() {
        // given
        given(userRoleRepository.findById(UserRole.Key(userId, roleId))).willReturn(Optional.of(assignment))
        given(roleApi.findRoles(listOf(roleId))).willReturn(listOf(memberRoleSummary(branchId).id(roleId).build()))

        // when / then
        assertThatThrownBy { testee.execute(userId, roleId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Member roles can't be manually deassigned!")
        then(userRoleRepository).should(never()).delete(anyValue<UserRole>())
    }

    @Test
    fun `should refuse when revoking the last admin`() {
        // given
        given(userRoleRepository.findById(UserRole.Key(userId, roleId))).willReturn(Optional.of(assignment))
        given(roleApi.findRoles(listOf(roleId))).willReturn(listOf(adminRoleSummary().id(roleId).build()))
        given(userRoleRepository.findByRoleId(roleId)).willReturn(listOf(assignment))

        // when / then
        assertThatThrownBy { testee.execute(userId, roleId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("At least one admin should exist!")
        then(userRoleRepository).should(never()).delete(anyValue<UserRole>())
    }

    @Test
    fun `should allow when another admin exists`() {
        // given
        val otherAdmin = userRole().userId(otherUserId).roleId(roleId).build()
        given(userRoleRepository.findById(UserRole.Key(userId, roleId))).willReturn(Optional.of(assignment))
        given(roleApi.findRoles(listOf(roleId))).willReturn(listOf(adminRoleSummary().id(roleId).build()))
        given(userRoleRepository.findByRoleId(roleId)).willReturn(listOf(assignment, otherAdmin))

        // when
        testee.execute(userId, roleId)

        // then
        then(userRoleRepository).should().delete(assignment)
    }

    @Test
    fun `should throw not found when assignment is missing`() {
        // given
        given(userRoleRepository.findById(UserRole.Key(userId, roleId))).willReturn(Optional.empty())

        // when / then
        assertThatThrownBy { testee.execute(userId, roleId) }
            .isInstanceOf(UserRoleNotFoundException::class.java)
        then(roleApi).shouldHaveNoInteractions()
    }
}
