package be.sgl.backend.service.user.inital

import be.sgl.backend.config.CustomUserDetailsTestMother.userDetails
import be.sgl.backend.entity.role.Role
import be.sgl.backend.entity.role.RoleTestMother.adminRole
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argThatValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CheckForInitialRunInternalTest {

    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var roleRepository: RoleRepository
    @InjectMocks
    lateinit var testee: CheckForInitialRunInternal

    @Test
    fun `should return null when users already exist`() {
        // given
        given(userRepository.count()).willReturn(1L)
        testee.checkInitialRun()

        // when
        val result = testee.execute(userDetails().build())

        // then
        assertThat(result).isNull()
        then(userRepository).should().count()
        then(userRepository).shouldHaveNoMoreInteractions()
        then(roleRepository).shouldHaveNoInteractions()
    }

    @Test
    fun `should create a user from the login details when it is the initial run`() {
        // given
        val details = userDetails().username("jdoe").firstName("John").lastName("Doe").email("john@doe.com").build()
        given(userRepository.count()).willReturn(0L)
        given(roleRepository.count()).willReturn(1L)
        given(userRepository.save(anyValue<User>())).willAnswer { it.arguments[0] }
        testee.checkInitialRun()

        // when
        val result = testee.execute(details)

        // then
        assertThat(result).extracting("username", "firstName", "name", "email")
            .containsExactly("jdoe", "John", "Doe", "john@doe.com")
    }

    @Test
    fun `should only create a user once when executed twice during the initial run`() {
        // given
        given(userRepository.count()).willReturn(0L)
        given(roleRepository.count()).willReturn(1L)
        given(userRepository.save(anyValue<User>())).willAnswer { it.arguments[0] }
        testee.checkInitialRun()
        testee.execute(userDetails().build())

        // when
        val second = testee.execute(userDetails().username("other").build())

        // then
        assertThat(second).isNull()
        then(userRepository).should().save(anyValue<User>())
    }

    @Test
    fun `should save an admin role when no roles exist`() {
        // given
        given(roleRepository.count()).willReturn(0L)

        // when
        testee.createAdminRoleIfNeeded()

        // then
        then(roleRepository).should().save(argThatValue<Role> { it.adminRole && it.externalId == null })
    }

    @Test
    fun `should give the initial user the admin level`() {
        // given
        given(userRepository.count()).willReturn(0L)
        given(roleRepository.count()).willReturn(0L)
        given(userRepository.save(anyValue<User>())).willAnswer { it.arguments[0] }
        testee.checkInitialRun()

        // when
        val result = testee.execute(userDetails().build())

        // then
        assertThat(result!!.level).isEqualTo(RoleLevel.ADMIN)
    }

    @Test
    fun `should give the initial user the existing admin role when roles already exist`() {
        // given
        val existingAdminRole = adminRole().build()
        given(userRepository.count()).willReturn(0L)
        given(roleRepository.count()).willReturn(1L)
        given(roleRepository.findByLevel(RoleLevel.ADMIN)).willReturn(mutableListOf(existingAdminRole))
        given(userRepository.save(anyValue<User>())).willAnswer { it.arguments[0] }
        testee.checkInitialRun()

        // when
        val result = testee.execute(userDetails().build())

        // then
        assertThat(result!!.roles).extracting("role").containsExactly(existingAdminRole)
    }
}
