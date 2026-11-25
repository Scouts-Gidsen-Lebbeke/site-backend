package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRole
import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argThatValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AssignRoleTest {

    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @InjectMocks
    lateinit var testee: AssignRole

    private val userId = 3
    private val roleId = 5

    @Test
    fun `should save assignment when user lacks the role`() {
        // given
        given(userRoleRepository.existsById(UserRole.Key(userId, roleId))).willReturn(false)

        // when
        val result = testee.execute(userId, roleId)

        // then
        assertThat(result).isTrue()
        then(userRoleRepository).should().save(argThatValue<UserRole> { it.userId == userId && it.roleId == roleId })
    }

    @Test
    fun `should return false when user already has the role`() {
        // given
        given(userRoleRepository.existsById(UserRole.Key(userId, roleId))).willReturn(true)

        // when
        val result = testee.execute(userId, roleId)

        // then
        assertThat(result).isFalse()
        then(userRoleRepository).should(never()).save(anyValue<UserRole>())
    }
}
