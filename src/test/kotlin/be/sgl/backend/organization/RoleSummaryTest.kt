package be.sgl.backend.organization

import be.sgl.backend.organization.RoleSummaryTestMother.roleSummary
import be.sgl.backend.shared.security.RoleLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RoleSummaryTest {

    private val branchId = 4
    private val staffBranchId = 5

    @Test
    fun `should be a member role for external sync when linked to a branch with guest level`() {
        // given
        val testee = roleSummary().branchId(branchId).build()

        // when
        val flags = listOf(testee.memberRole, testee.forExternalSync, testee.staffRole, testee.adminRole)

        // then
        assertThat(flags).containsExactly(true, true, false, false)
    }

    @Test
    fun `should not be a member role when a branch role has a level above guest`() {
        // given
        val testee = roleSummary().branchId(branchId).level(RoleLevel.STAFF).build()

        // when
        val memberRole = testee.memberRole

        // then
        assertThat(memberRole).isFalse()
    }

    @Test
    fun `should be neither member nor staff role when linked to both a branch and a staff branch`() {
        // given
        val testee = roleSummary().branchId(branchId).staffBranchId(staffBranchId).build()

        // when
        val flags = listOf(testee.memberRole, testee.staffRole)

        // then
        assertThat(flags).containsExactly(false, false)
    }

    @Test
    fun `should be a staff role but not for external sync when linked to a staff branch`() {
        // given
        val testee = roleSummary().staffBranchId(staffBranchId).level(RoleLevel.STAFF).externalId("staffExternalId").build()

        // when
        val flags = listOf(testee.staffRole, testee.forExternalSync, testee.memberRole, testee.adminRole)

        // then
        assertThat(flags).containsExactly(true, false, false, false)
    }

    @Test
    fun `should be an admin role when the level is admin`() {
        // given
        val testee = roleSummary().level(RoleLevel.ADMIN).build()

        // when
        val flags = listOf(testee.adminRole, testee.memberRole, testee.staffRole, testee.forExternalSync)

        // then
        assertThat(flags).containsExactly(true, false, false, false)
    }

    @Test
    fun `should not be an admin role when the level is staff`() {
        // given
        val testee = roleSummary().level(RoleLevel.STAFF).build()

        // when
        val adminRole = testee.adminRole

        // then
        assertThat(adminRole).isFalse()
    }
}
