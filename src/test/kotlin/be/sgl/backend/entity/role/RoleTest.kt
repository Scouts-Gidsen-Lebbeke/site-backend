package be.sgl.backend.entity.role

import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.role.RoleTestMother.memberRole
import be.sgl.backend.entity.role.RoleTestMother.role
import be.sgl.backend.entity.role.RoleTestMother.staffRole
import be.sgl.backend.shared.security.RoleLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RoleTest {

    @Nested
    inner class ComputedProperties {

        @Test
        fun `should be a member role for external sync when linked to a branch with guest level`() {
            // given
            val testee = memberRole(branch().build()).build()

            // when
            val flags = listOf(testee.memberRole, testee.forExternalSync, testee.staffRole, testee.adminRole)

            // then
            assertThat(flags).containsExactly(true, true, false, false)
        }

        @Test
        fun `should not be a member role when a branch role has a level above guest`() {
            // given
            val testee = memberRole(branch().build()).level(RoleLevel.STAFF).build()

            // when
            val memberRole = testee.memberRole

            // then
            assertThat(memberRole).isFalse()
        }

        @Test
        fun `should be neither member nor staff role when linked to both a branch and a staff branch`() {
            // given
            val testee = role().branch(branch().build()).staffBranch(branch().id(2).build()).build()

            // when
            val flags = listOf(testee.memberRole, testee.staffRole)

            // then
            assertThat(flags).containsExactly(false, false)
        }

        @Test
        fun `should be a staff role but not for external sync when linked to a staff branch`() {
            // given
            val testee = staffRole(branch().build()).externalId("staffExternalId").build()

            // when
            val flags = listOf(testee.staffRole, testee.forExternalSync, testee.memberRole, testee.adminRole)

            // then
            assertThat(flags).containsExactly(true, false, false, false)
        }

        @Test
        fun `should be an admin role when the level is admin`() {
            // given
            val testee = adminRole().build()

            // when
            val flags = listOf(testee.adminRole, testee.memberRole, testee.staffRole, testee.forExternalSync)

            // then
            assertThat(flags).containsExactly(true, false, false, false)
        }

        @Test
        fun `should not be an admin role when the level is staff`() {
            // given
            val testee = role().level(RoleLevel.STAFF).build()

            // when
            val adminRole = testee.adminRole

            // then
            assertThat(adminRole).isFalse()
        }
    }

    @Nested
    inner class Factories {

        @Test
        fun `should create a guest member role linked to the branch when creating a member role`() {
            // given
            val branch = branch().build()

            // when
            val testee = Role.memberRole("Kapoenen", "extId", "backupId", branch)

            // then
            assertThat(testee).extracting("name", "externalId", "backupExternalId", "branch", "staffBranch", "level")
                .containsExactly("Kapoenen", "extId", "backupId", branch, null, RoleLevel.GUEST)
            assertThat(testee.memberRole).isTrue()
        }

        @Test
        fun `should create a staff level role when creating a staff role with staff level`() {
            // given
            val branch = branch().build()

            // when
            val testee = Role.staffRole("Leiding", "extId", null, branch, true)

            // then
            assertThat(testee).extracting("name", "externalId", "backupExternalId", "branch", "staffBranch", "level")
                .containsExactly("Leiding", "extId", null, null, branch, RoleLevel.STAFF)
            assertThat(testee.staffRole).isTrue()
        }

        @Test
        fun `should create a guest level role when creating a staff role without staff level`() {
            // given
            val branch = branch().build()

            // when
            val testee = Role.staffRole("Leiding", null, null, branch, false)

            // then
            assertThat(testee.level).isEqualTo(RoleLevel.GUEST)
            assertThat(testee.staffRole).isTrue()
        }

        @Test
        fun `should create an admin role without branches when creating an admin role`() {
            // given

            // when
            val testee = Role.adminRole("Admin", "vga", "avga")

            // then
            assertThat(testee).extracting("name", "externalId", "backupExternalId", "branch", "staffBranch", "level")
                .containsExactly("Admin", "vga", "avga", null, null, RoleLevel.ADMIN)
            assertThat(testee.adminRole).isTrue()
        }
    }
}
