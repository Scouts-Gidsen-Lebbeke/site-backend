package be.sgl.backend.repository.user

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.access.domain.UserRoleTestMother.userRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.domain.Role
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.organization.domain.RoleTestMother.memberRole
import be.sgl.backend.organization.domain.RoleTestMother.staffRole
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.util.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class UserRepositoryTest {

    @Autowired
    private lateinit var testee: UserRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository

    @AfterEach
    fun cleanUp() {
        userRoleRepository.deleteAll()
        testee.deleteAll()
        roleRepository.deleteAll()
        branchRepository.deleteAll()
    }

    private fun persistedUserHolding(vararg roles: Role): User {
        val user = testee.save(user().id(null).username(USERNAME).build())
        roles.map(roleRepository::save).forEach { userRoleRepository.save(userRole().userId(user.id!!).roleId(it.id!!).build()) }
        return user
    }

    @Test
    fun `should read guest level when the user holds no role`() {
        // given
        persistedUserHolding()

        // when
        val user = testee.findByUsername(USERNAME)

        // then
        assertThat(user!!.level).isEqualTo(RoleLevel.GUEST)
    }

    @Test
    fun `should read guest level when the user only holds a member role`() {
        // given
        val branch = branchRepository.save(branch().id(null).build())
        persistedUserHolding(memberRole(branch).id(null).build())

        // when
        val user = testee.findByUsername(USERNAME)

        // then
        assertThat(user!!.level).isEqualTo(RoleLevel.GUEST)
    }

    @Test
    fun `should read staff level when the user holds a staff role`() {
        // given
        val branch = branchRepository.save(branch().id(null).build())
        persistedUserHolding(staffRole(branch).id(null).build())

        // when
        val user = testee.findByUsername(USERNAME)

        // then
        assertThat(user!!.level).isEqualTo(RoleLevel.STAFF)
    }

    @Test
    fun `should read the highest level when the user holds staff and admin roles`() {
        // given
        val branch = branchRepository.save(branch().id(null).build())
        persistedUserHolding(staffRole(branch).id(null).build(), adminRole().id(null).build())

        // when
        val user = testee.findByUsername(USERNAME)

        // then
        assertThat(user!!.level).isEqualTo(RoleLevel.ADMIN)
    }

    companion object {
        private const val USERNAME = "holder"
    }
}
