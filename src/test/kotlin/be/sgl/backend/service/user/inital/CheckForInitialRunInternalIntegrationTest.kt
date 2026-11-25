package be.sgl.backend.service.user.inital

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.people.domain.UserRepository
import be.sgl.backend.shared.config.CustomUserDetailsTestMother.userDetails
import be.sgl.backend.shared.security.RoleLevel
import be.sgl.backend.shared.util.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class CheckForInitialRunInternalIntegrationTest {

    @Autowired
    private lateinit var testee: CheckForInitialRunInternal
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @AfterEach
    fun cleanUp() {
        userRoleRepository.deleteAll()
        userRepository.deleteAll()
        roleRepository.deleteAll()
    }

    @Test
    fun `should return first user with admin level when no users exist`() {
        // given
        testee.checkInitialRun()

        // when
        val result = testee.execute(userDetails().username("first").build())

        // then
        assertThat(result!!.level).isEqualTo(RoleLevel.ADMIN)
    }
}
