package be.sgl.backend.access.application

import be.sgl.backend.access.domain.UserRoleRepository
import be.sgl.backend.organization.RoleDeleting
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class DeleteAssignmentsOnRoleDeletingTest {

    @Mock
    lateinit var userRoleRepository: UserRoleRepository
    @InjectMocks
    lateinit var testee: DeleteAssignmentsOnRoleDeleting

    private val roleId = 2

    @Test
    fun `should delete the assignments of the role when a role is being deleted`() {
        // given
        val event = RoleDeleting(roleId)

        // when
        testee.on(event)

        // then
        then(userRoleRepository).should().deleteByRoleId(roleId)
    }
}
