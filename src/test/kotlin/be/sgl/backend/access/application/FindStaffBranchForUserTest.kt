package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.organization.BranchApi
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummaryTestMother.adminRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.memberRoleSummary
import be.sgl.backend.organization.RoleSummaryTestMother.staffRoleSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FindStaffBranchForUserTest {

    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @Mock
    lateinit var roleApi: RoleApi
    @Mock
    lateinit var branchApi: BranchApi
    @InjectMocks
    lateinit var testee: FindStaffBranchForUser

    private val userId = 3
    private val memberRoleId = 1
    private val inactiveStaffRoleId = 2
    private val activeStaffRoleId = 3
    private val inactiveBranchId = 10
    private val activeBranchId = 11

    @Test
    fun `should return active staff branch`() {
        // given
        given(userRoleRepository.findByUserId(userId)).willReturn(listOf(
            userRole().userId(userId).roleId(memberRoleId).build(),
            userRole().userId(userId).roleId(activeStaffRoleId).build()
        ))
        given(roleApi.findRoles(listOf(memberRoleId, activeStaffRoleId))).willReturn(listOf(
            memberRoleSummary(activeBranchId).id(memberRoleId).build(),
            staffRoleSummary(activeBranchId).id(activeStaffRoleId).build()
        ))
        given(branchApi.isActive(activeBranchId)).willReturn(true)

        // when
        val result = testee.execute(userId)

        // then
        assertThat(result).isEqualTo(activeBranchId)
    }

    @Test
    fun `should skip inactive branch`() {
        // given
        given(userRoleRepository.findByUserId(userId)).willReturn(listOf(
            userRole().userId(userId).roleId(inactiveStaffRoleId).build(),
            userRole().userId(userId).roleId(activeStaffRoleId).build()
        ))
        given(roleApi.findRoles(listOf(inactiveStaffRoleId, activeStaffRoleId))).willReturn(listOf(
            staffRoleSummary(inactiveBranchId).id(inactiveStaffRoleId).build(),
            staffRoleSummary(activeBranchId).id(activeStaffRoleId).build()
        ))
        given(branchApi.isActive(inactiveBranchId)).willReturn(false)
        given(branchApi.isActive(activeBranchId)).willReturn(true)

        // when
        val result = testee.execute(userId)

        // then
        assertThat(result).isEqualTo(activeBranchId)
    }

    @Test
    fun `should return null when user has no staff role`() {
        // given
        val adminRoleId = 4
        given(userRoleRepository.findByUserId(userId)).willReturn(listOf(userRole().userId(userId).roleId(adminRoleId).build()))
        given(roleApi.findRoles(listOf(adminRoleId))).willReturn(listOf(adminRoleSummary().id(adminRoleId).build()))

        // when
        val result = testee.execute(userId)

        // then
        assertThat(result).isNull()
        then(branchApi).shouldHaveNoInteractions()
    }
}
