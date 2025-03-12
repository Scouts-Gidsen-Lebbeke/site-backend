package be.sgl.backend.config

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserRole
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.repository.user.UserRoleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDate

@Configuration
@Profile("local")
class DataLoader {

    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var userRoleRepository: UserRoleRepository

    @Bean
    fun loadData() = CommandLineRunner {
        val kapoenen = createBranch("Kapoenen", 7, 8)
        val welpen = createBranch("Welpen", 9, 11)
        val jonggivers = createBranch("Jonggivers", 12, 14)
        val givers = createBranch("Givers", 15, 17)
        val jins = createBranch("Jins", 18, 18)
        val staff = createBranch("Leiding", 19, null, BranchStatus.MEMBER)
        if (branchRepository.findAll().isEmpty()) {
            branchRepository.saveAll(listOf(kapoenen, welpen, jonggivers, givers, jins, staff))
        }
        var adminRole = createRole("VGA", "d5f75b320b812440010b812555970393", level = RoleLevel.ADMIN)
        if (roleRepository.findAll().isEmpty()) {
            adminRole = roleRepository.save(adminRole)
            roleRepository.saveAll(listOf(
                createRole("AVGA", "8a95af9385ad9b880185c035ee740010", level = RoleLevel.ADMIN),
                createRole("Kapoen", "d5f75b320b812440010b812555de03a2"),
                createRole("Welp", "d5f75b320b812440010b8125567703cb"),
                createRole("Jonggiver", "d5f75b320b812440010b812555d603a0"),
                createRole("Giver", "d5f75b320b812440010b8125565203c1"),
                createRole("Jin", "d5f75b320b812440010b812555c1039b"),
                createRole("Kapoenenleiding", "d5f75b320b812440010b812555e603a4", kapoenen, RoleLevel.STAFF),
                createRole("Welpenleiding", "d5f75b320b812440010b812555ec03a5", welpen, RoleLevel.STAFF),
                createRole("Jonggiverleiding", "d5f75b320b812440010b812555cc039e", jonggivers, RoleLevel.STAFF),
                createRole("Giverleiding", "d5f75b320b812440010b812555b50398", givers, RoleLevel.STAFF),
                createRole("Jinleiding", "d5f75b320b812440010b812555d2039f", jins, RoleLevel.STAFF),
                createRole("Groepsleiding", "d5f75b320b812440010b8125558e0391", staff, RoleLevel.STAFF)
            ))
        }
        if (userRepository.findAll().isEmpty()) {
            val rootUser = userRepository.save(User().apply {
                firstName = "root"
                name = "user"
                username = "admin"
            })
            userRoleRepository.save(UserRole(rootUser, adminRole, LocalDate.now()))
        }
    }

    private fun createRole(name: String, externalId: String, staffBranch: Branch? = null, level: RoleLevel = RoleLevel.GUEST) = Role().apply {
        this.name = name
        this.externalId = externalId
        this.staffBranch = staffBranch
        this.level = level
    }

    private fun createBranch(name: String, min: Int, max: Int?, status: BranchStatus = BranchStatus.ACTIVE) = Branch().apply {
        this.name = name
        this.email = "${name.lowercase()}@mygroup.com"
        this.minimumAge = min
        this.maximumAge = max
        this.image = "${name.lowercase()}.png"
        this.status = status
    }
}
