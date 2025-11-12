package be.sgl.backend.config

import be.sgl.backend.entity.user.Role.Companion.adminRole
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class InitialRunChecker(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    private var isInitialRun = false

    @PostConstruct
    fun checkInitialRun() {
        isInitialRun = userRepository.count() == 0L
    }

    private fun createAdminRoleIfNeeded(externalSync: Boolean) {
        if (roleRepository.count() == 0L) {
            adminRole("Admin", VGA_FUNCTION.takeIf { externalSync }, AVGA_FUNCTION.takeIf { externalSync })
        }
    }

    companion object {
        const val VGA_FUNCTION = "d5f75b320b812440010b812555970393"
        const val AVGA_FUNCTION = "8a95af9385ad9b880185c035ee740010"
    }
}