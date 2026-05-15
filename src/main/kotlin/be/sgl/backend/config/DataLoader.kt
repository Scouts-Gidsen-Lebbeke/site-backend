package be.sgl.backend.config

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Role.Companion.adminRole
import be.sgl.backend.entity.user.Role.Companion.memberRole
import be.sgl.backend.entity.user.Role.Companion.staffRole
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.RoleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DataLoader {

    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository
    @Value("\${organization.external.id}")
    var externalOrganizationId: String? = null

    @Bean
    fun loadData() = CommandLineRunner {
        // Suppose a clean first run only when no branches are configured
        if (branchRepository.findAll().isNotEmpty()) {
            return@CommandLineRunner
        }

        val kapoenen = createBranch("Kapoenen", 7, 8)
        val welpen = createBranch("Welpen", 9, 11)
        val jonggivers = createBranch("Jonggivers", 12, 14)
        val givers = createBranch("Givers", 15, 17)
        val jins = createBranch("Jins", 18, 18)
        val staff = createBranch("Leiding", 19, null, BranchStatus.MEMBER)
        branchRepository.saveAll(listOf(kapoenen, welpen, jonggivers, givers, jins, staff))

        val externalSync = externalOrganizationId != null
        roleRepository.saveAll(listOf(
            adminRole("Admin", "d5f75b320b812440010b812555970393".takeIf { externalSync }, "8a95af9385ad9b880185c035ee740010".takeIf { externalSync }),
            staffRole("Kapoenenleiding", "d5f75b320b812440010b812555e603a4".takeIf { externalSync }, null, kapoenen, true),
            staffRole("Welpenleiding", "d5f75b320b812440010b812555ec03a5".takeIf { externalSync }, null, welpen, true),
            staffRole("Jonggiverleiding", "d5f75b320b812440010b812555cc039e".takeIf { externalSync }, null, jonggivers, true),
            staffRole("Giverleiding", "d5f75b320b812440010b812555b50398".takeIf { externalSync }, null, givers, true),
            staffRole("Jinleiding", "d5f75b320b812440010b812555d2039f".takeIf { externalSync }, null, jins, true),
            staffRole("Groepsleiding", "d5f75b320b812440010b8125558e0391".takeIf { externalSync }, null, staff, true)
        ))
        if (externalSync) {
            roleRepository.saveAll(listOf(
                memberRole("Kapoen", "d5f75b320b812440010b812555de03a2", null, kapoenen),
                memberRole("Welp", "d5f75b320b812440010b8125567703cb", null, welpen),
                memberRole("Jonggiver", "d5f75b320b812440010b812555d603a0", null, jonggivers),
                memberRole("Giver", "d5f75b320b812440010b8125565203c1", null, givers),
                memberRole("Jin", "d5f75b320b812440010b812555c1039b", null, jins)
            ))
        }
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
