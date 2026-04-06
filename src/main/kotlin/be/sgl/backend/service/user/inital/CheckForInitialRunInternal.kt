package be.sgl.backend.service.user.inital

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.entity.role.Role.Companion.adminRole
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.role.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.util.ForInternalOrganization
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@ForInternalOrganization
class CheckForInitialRunInternal(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
) : CheckForInitialRun {

    private var isInitialRun = false

    @PostConstruct
    fun checkInitialRun() {
        isInitialRun = userRepository.count() == 0L
    }

    override fun execute(userDetails: CustomUserDetails): User? {
        if (!isInitialRun) return null
        createAdminRoleIfNeeded()
        var user = User().apply {
            name = userDetails.lastName
            firstName = userDetails.firstName
            email = userDetails.email
            username = userDetails.username
            birthdate = LocalDate.now() // non-null
        }
        user = userRepository.save(user)
        isInitialRun = false
        return user
    }

    fun createAdminRoleIfNeeded() {
        if (roleRepository.count() == 0L) {
            adminRole("Admin", null, null)
        }
    }
}