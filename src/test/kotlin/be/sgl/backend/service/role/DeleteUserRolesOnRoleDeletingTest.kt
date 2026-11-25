package be.sgl.backend.service.role

import be.sgl.backend.organization.RoleDeleting
import be.sgl.backend.repository.role.UserRoleRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DeleteUserRolesOnRoleDeletingTest {

    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @InjectMocks
    lateinit var testee: DeleteUserRolesOnRoleDeleting

    private val roleId = 2

    @Test
    fun `should delete the user roles of the role when a role is being deleted`() {
        // given
        val event = RoleDeleting(roleId)

        // when
        testee.on(event)

        // then
        then(userRoleRepository).should().deleteUserRolesByRoleId(roleId)
    }
}
