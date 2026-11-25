package be.sgl.backend.organization.application

import be.sgl.backend.organization.RoleSummary
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.shared.security.RoleLevel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class RoleApiImplTest {

    @Mock
    lateinit var roleRepository: RoleRepository
    @InjectMocks
    lateinit var testee: RoleApiImpl

    private val branch = branch().id(3).build()

    @Test
    fun `should map role fields when finding roles by id`() {
        // given
        val role = staffRole(branch).id(2).name("Leiding").externalId("ext").backupExternalId("backup").build()
        val ids = listOf(role.id!!)
        given(roleRepository.findAllById(ids)).willReturn(listOf(role))

        // when
        val result = testee.findRoles(ids)

        // then
        assertThat(result).containsExactly(
            RoleSummary(role.id!!, role.name, role.level, null, branch.id, role.externalId, role.backupExternalId)
        )
    }

    @Test
    fun `should return all staff roles when a branch has several`() {
        // given
        val staffLevelRole = staffRole(branch).id(2).build()
        val guestLevelRole = staffRole(branch).id(5).level(RoleLevel.GUEST).build()
        given(roleRepository.findByStaffBranchId(branch.id!!)).willReturn(listOf(staffLevelRole, guestLevelRole))

        // when
        val result = testee.findStaffRoles(branch.id!!)

        // then
        assertThat(result).extracting<Int> { it.id }.containsExactly(staffLevelRole.id, guestLevelRole.id)
    }
}
