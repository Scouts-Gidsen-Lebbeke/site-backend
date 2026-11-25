package be.sgl.backend.service.role

import be.sgl.backend.access.AccessApi
import be.sgl.backend.organization.domain.RoleTestMother.adminRole
import be.sgl.backend.people.domain.UserTestMother.user
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class RoleAssignerTest {

    @Mock
    lateinit var accessApi: AccessApi
    @InjectMocks
    lateinit var testee: RoleAssigner

    private val user = user().id(1).build()
    private val role = adminRole().id(2).build()

    @Test
    fun `should assign through access when starting a role`() {
        // given
        given(accessApi.assign(user.id!!, role.id!!)).willReturn(true)

        // when
        val result = testee.startRole(user, role)

        // then
        assertThat(result).isTrue()
        then(accessApi).should().assign(user.id!!, role.id!!)
    }

    @Test
    fun `should return false when role already assigned`() {
        // given
        given(accessApi.assign(user.id!!, role.id!!)).willReturn(false)

        // when
        val result = testee.startRole(user, role)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `should revoke through access when ending a role`() {
        // given

        // when
        testee.endRole(user, role)

        // then
        then(accessApi).should().revoke(user.id!!, role.id!!)
    }
}
