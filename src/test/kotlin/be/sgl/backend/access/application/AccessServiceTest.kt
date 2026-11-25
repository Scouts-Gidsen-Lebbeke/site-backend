package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.organization.RoleApi
import be.sgl.backend.organization.RoleSummaryTestMother.staffRoleSummary
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
class AccessServiceTest {

    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @Mock
    lateinit var roleApi: RoleApi
    @Mock
    lateinit var assignRole: AssignRole
    @Mock
    lateinit var revokeRole: RevokeRole
    @Mock
    lateinit var findStaffBranchForUser: FindStaffBranchForUser
    @InjectMocks
    lateinit var testee: AccessService

    private val userId = 3
    private val otherUserId = 4
    private val roleId = 5
    private val branchId = 7

    @Nested
    inner class StaffUserIdsOf {

        @Test
        fun `should return each holder once when the branch has several staff roles`() {
            // given
            val leaderRoleId = 1
            val assistantRoleId = 2
            given(roleApi.findStaffRoles(branchId)).willReturn(listOf(
                staffRoleSummary(branchId).id(leaderRoleId).build(),
                staffRoleSummary(branchId).id(assistantRoleId).build()
            ))
            given(userRoleRepository.findByRoleIdIn(listOf(leaderRoleId, assistantRoleId))).willReturn(listOf(
                userRole().userId(userId).roleId(leaderRoleId).build(),
                userRole().userId(otherUserId).roleId(assistantRoleId).build(),
                userRole().userId(userId).roleId(assistantRoleId).build()
            ))

            // when
            val result = testee.staffUserIdsOf(branchId)

            // then
            assertThat(result).containsExactly(userId, otherUserId)
        }

        @Test
        fun `should return no holders when the branch has no staff roles`() {
            // given
            given(roleApi.findStaffRoles(branchId)).willReturn(emptyList())

            // when
            val result = testee.staffUserIdsOf(branchId)

            // then
            assertThat(result).isEmpty()
            then(userRoleRepository).shouldHaveNoInteractions()
        }
    }

    @Test
    fun `should return the role ids of the user when listing its roles`() {
        // given
        given(userRoleRepository.findByUserId(userId)).willReturn(listOf(userRole().userId(userId).roleId(roleId).build()))

        // when
        val result = testee.roleIdsOf(userId)

        // then
        assertThat(result).containsExactly(roleId)
    }

    @Test
    fun `should return the user ids of the holders when listing users with a role`() {
        // given
        given(userRoleRepository.findByRoleId(roleId)).willReturn(listOf(
            userRole().userId(userId).roleId(roleId).build(),
            userRole().userId(otherUserId).roleId(roleId).build()
        ))

        // when
        val result = testee.userIdsWithRole(roleId)

        // then
        assertThat(result).containsExactly(userId, otherUserId)
    }
}
