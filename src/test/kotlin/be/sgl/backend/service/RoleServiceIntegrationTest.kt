package be.sgl.backend.service

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.RoleDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Role
import be.sgl.backend.entity.user.RoleLevel
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.service.exception.RoleNotFoundException
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class RoleServiceIntegrationTest {

    @Autowired
    private lateinit var roleService: RoleService

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var branchRepository: BranchRepository

    private lateinit var testBranch: Branch

    @BeforeEach
    fun setup() {
        roleRepository.deleteAll()
        branchRepository.deleteAll()

        testBranch = Branch().apply {
            name = "Test Branch"
            email = "test@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        testBranch = branchRepository.save(testBranch)
    }

    @Test
    fun `getAllRoles should return all roles from database`() {
        val role1 = Role().apply {
            name = "Leader"
            level = RoleLevel.STAFF
        }
        val role2 = Role().apply {
            name = "Assistant"
            level = RoleLevel.STAFF
        }

        roleRepository.save(role1)
        roleRepository.save(role2)
        roleRepository.flush()

        val roles = roleService.getAllRoles()

        assertTrue(roles.size >= 2)
        assertTrue(roles.any { it.name == "Leader" })
        assertTrue(roles.any { it.name == "Assistant" })
    }

    @Test
    fun `saveRoleDTO should persist role to database`() {
        val dto = RoleDTO(
            id = null,
            name = "New Role",
            externalId = null,
            backupExternalId = null,
            branch = null,
            staffBranch = null,
            level = RoleLevel.STAFF
        )

        val saved = roleService.saveRoleDTO(dto)

        assertNotNull(saved.id)
        assertEquals("New Role", saved.name)
        assertEquals(RoleLevel.STAFF, saved.level)

        // Verify it's in the database
        val fromDb = roleRepository.findById(saved.id!!).get()
        assertEquals("New Role", fromDb.name)
    }

    @Test
    fun `mergeRoleDTOChanges should update existing role`() {
        val role = Role().apply {
            name = "Original Role"
            level = RoleLevel.STAFF
        }
        val savedRole = roleRepository.save(role)
        roleRepository.flush()

        val dto = RoleDTO(
            id = savedRole.id,
            name = "Updated Role",
            externalId = "ext-123",
            backupExternalId = "backup-456",
            branch = null,
            staffBranch = null,
            level = RoleLevel.ADMIN
        )

        val updated = roleService.mergeRoleDTOChanges(savedRole.id!!, dto)

        assertEquals("Updated Role", updated.name)
        assertEquals("ext-123", updated.externalId)
        assertEquals("backup-456", updated.backupExternalId)
        assertEquals(RoleLevel.ADMIN, updated.level)

        // Verify changes are persisted
        val fromDb = roleRepository.findById(savedRole.id!!).get()
        assertEquals("Updated Role", fromDb.name)
        assertEquals(RoleLevel.ADMIN, fromDb.level)
    }

    @Test
    fun `mergeRoleDTOChanges should update branch associations`() {
        val role = Role().apply {
            name = "Role with Branch"
            level = RoleLevel.STAFF
        }
        val savedRole = roleRepository.save(role)
        roleRepository.flush()

        val dto = RoleDTO(
            id = savedRole.id,
            name = "Role with Branch",
            externalId = null,
            backupExternalId = null,
            branch = be.sgl.backend.dto.BranchBaseDTO(
                id = testBranch.id,
                name = testBranch.name,
                description = testBranch.description,
                image = testBranch.image,
                minimumAge = testBranch.minimumAge,
                maximumAge = testBranch.maximumAge,
                sex = testBranch.sex
            ),
            staffBranch = null,
            level = RoleLevel.STAFF
        )

        val updated = roleService.mergeRoleDTOChanges(savedRole.id!!, dto)

        assertNotNull(updated.branch)
        assertEquals(testBranch.id, updated.branch?.id)
    }

    @Test
    fun `deleteRole should remove role from database`() {
        val role = Role().apply {
            name = "To Delete"
            level = RoleLevel.STAFF
        }
        val savedRole = roleRepository.save(role)
        roleRepository.flush()

        roleService.deleteRole(savedRole.id!!)

        assertFalse(roleRepository.existsById(savedRole.id!!))
    }

    @Test
    fun `deleteRole should throw exception when role not found`() {
        assertThrows(RoleNotFoundException::class.java) {
            roleService.deleteRole(999)
        }
    }

    @Test
    fun `mergeRoleDTOChanges should throw exception when role not found`() {
        val dto = RoleDTO(
            id = 999,
            name = "Non-existent",
            externalId = null,
            backupExternalId = null,
            branch = null,
            staffBranch = null,
            level = RoleLevel.STAFF
        )

        assertThrows(RoleNotFoundException::class.java) {
            roleService.mergeRoleDTOChanges(999, dto)
        }
    }

    @Test
    fun `mergeRoleDTOChanges should throw exception when branch not found`() {
        val role = Role().apply {
            name = "Role"
            level = RoleLevel.STAFF
        }
        val savedRole = roleRepository.save(role)
        roleRepository.flush()

        val dto = RoleDTO(
            id = savedRole.id,
            name = "Role",
            externalId = null,
            backupExternalId = null,
            branch = be.sgl.backend.dto.BranchBaseDTO(
                id = 999, // Non-existent branch
                name = "Fake",
                description = "Fake",
                image = "fake.jpg",
                minimumAge = 6,
                maximumAge = 8,
                sex = Sex.UNKNOWN
            ),
            staffBranch = null,
            level = RoleLevel.STAFF
        )

        assertThrows(BranchNotFoundException::class.java) {
            roleService.mergeRoleDTOChanges(savedRole.id!!, dto)
        }
    }

    @Test
    fun `saveRoleDTO should handle all role levels`() {
        val levels = listOf(
            RoleLevel.GUEST,
            RoleLevel.STAFF,
            RoleLevel.ADMIN
        )

        levels.forEach { level ->
            val dto = RoleDTO(
                id = null,
                name = "Role for $level",
                externalId = null,
                backupExternalId = null,
                branch = null,
                staffBranch = null,
                level = level
            )

            val saved = roleService.saveRoleDTO(dto)
            assertEquals(level, saved.level)
        }
    }

    @Test
    fun `roles should persist with external IDs`() {
        val dto = RoleDTO(
            id = null,
            name = "External Role",
            externalId = "ext-abc-123",
            backupExternalId = "backup-xyz-789",
            branch = null,
            staffBranch = null,
            level = RoleLevel.STAFF
        )

        val saved = roleService.saveRoleDTO(dto)

        assertEquals("ext-abc-123", saved.externalId)
        assertEquals("backup-xyz-789", saved.backupExternalId)

        // Verify persistence
        val fromDb = roleRepository.findById(saved.id!!).get()
        assertEquals("ext-abc-123", fromDb.externalId)
        assertEquals("backup-xyz-789", fromDb.backupExternalId)
    }
}
