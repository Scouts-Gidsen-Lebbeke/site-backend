package be.sgl.backend.service

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.util.IntegrationTest
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@IntegrationTest
@Import(TestConfigurations::class)
@Transactional
class BranchServiceIntegrationTest {

    @Autowired
    private lateinit var branchService: BranchService

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Test
    fun `getAllBranches should return all branches`() {
        val branch1 = Branch().apply {
            name = "Branch 1"
            email = "branch1@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Test 1"
            law = "Law 1"
            image = "test1.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val branch2 = Branch().apply {
            name = "Branch 2"
            email = "branch2@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Test 2"
            law = "Law 2"
            image = "test2.jpg"
            status = BranchStatus.HIDDEN
            staffTitle = "Coach"
        }

        branchRepository.save(branch1)
        branchRepository.save(branch2)
        branchRepository.flush()

        val branches = branchService.getAllBranches()

        assertTrue(branches.size >= 2)
        assertTrue(branches.any { it.name == "Branch 1" })
        assertTrue(branches.any { it.name == "Branch 2" })
    }

    @Test
    fun `getVisibleBranches should only return non-hidden branches`() {
        val visibleBranch = Branch().apply {
            name = "Visible Branch"
            email = "visible@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Visible"
            law = "Law"
            image = "visible.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val hiddenBranch = Branch().apply {
            name = "Hidden Branch"
            email = "hidden@example.com"
            minimumAge = 9
            maximumAge = 11
            sex = Sex.UNKNOWN
            description = "Hidden"
            law = "Law"
            image = "hidden.jpg"
            status = BranchStatus.HIDDEN
            staffTitle = "Coach"
        }

        branchRepository.save(visibleBranch)
        branchRepository.save(hiddenBranch)
        branchRepository.flush()

        val branches = branchService.getVisibleBranches()

        assertTrue(branches.any { it.name == "Visible Branch" })
        assertFalse(branches.any { it.name == "Hidden Branch" })
    }

    @Test
    fun `getBranchDTOById should return branch with staff`() {
        val branch = Branch().apply {
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
        val savedBranch = branchRepository.save(branch)
        branchRepository.flush()

        val branchDTO = branchService.getBranchDTOById(savedBranch.id!!)

        assertNotNull(branchDTO)
        assertEquals("Test Branch", branchDTO.name)
        assertEquals("test@example.com", branchDTO.email)
        assertEquals(6, branchDTO.minimumAge)
        assertEquals(8, branchDTO.maximumAge)
    }

    @Test
    fun `mergeBranchDTOChanges should update branch properties`() {
        val branch = Branch().apply {
            name = "Original Branch"
            email = "original@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.MALE
            description = "Original description"
            law = "Original law"
            image = "original.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        val savedBranch = branchRepository.save(branch)
        branchRepository.flush()

        val updatedDTO = BranchDTO(
            id = savedBranch.id,
            name = "Updated Branch",
            email = "updated@example.com",
            minimumAge = 7,
            maximumAge = 9,
            sex = Sex.FEMALE,
            description = "Updated description",
            law = "Updated law",
            image = "original.jpg",
            status = BranchStatus.PASSIVE,
            staffTitle = "Coach",
            staff = emptyList()
        )

        val result = branchService.mergeBranchDTOChanges(savedBranch.id!!, updatedDTO)

        assertEquals("Updated Branch", result.name)
        assertEquals("updated@example.com", result.email)
        assertEquals(7, result.minimumAge)
        assertEquals(9, result.maximumAge)
        assertEquals(Sex.FEMALE, result.sex)
        assertEquals(BranchStatus.PASSIVE, result.status)
    }

    @Test
    fun `getPossibleBranchesForSexAndAge should return matching branches`() {
        val branch1 = Branch().apply {
            name = "Young Boys"
            email = "young@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.MALE
            description = "For young boys"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        val branch2 = Branch().apply {
            name = "Older Girls"
            email = "older@example.com"
            minimumAge = 12
            maximumAge = 15
            sex = Sex.FEMALE
            description = "For older girls"
            law = "Law"
            image = "test2.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        branchRepository.save(branch1)
        branchRepository.save(branch2)
        branchRepository.flush()

        val matchingBranches = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 7)

        assertEquals(1, matchingBranches.size)
        assertEquals("Young Boys", matchingBranches[0].name)
    }

    @Test
    fun `getPossibleBranchesForSexAndAge should include UNKNOWN sex branches`() {
        val mixedBranch = Branch().apply {
            name = "Mixed Branch"
            email = "mixed@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "For all"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        branchRepository.save(mixedBranch)
        branchRepository.flush()

        val matchingBoys = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 7)
        val matchingGirls = branchRepository.getPossibleBranchesForSexAndAge(Sex.FEMALE, 7)

        assertTrue(matchingBoys.any { it.name == "Mixed Branch" })
        assertTrue(matchingGirls.any { it.name == "Mixed Branch" })
    }

    @Test
    fun `branch should not match when age is outside range`() {
        val branch = Branch().apply {
            name = "Specific Age Branch"
            email = "age@example.com"
            minimumAge = 10
            maximumAge = 12
            sex = Sex.UNKNOWN
            description = "For specific ages"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }

        branchRepository.save(branch)
        branchRepository.flush()

        val tooYoung = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 8)
        val tooOld = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 15)

        assertFalse(tooYoung.any { it.name == "Specific Age Branch" })
        assertFalse(tooOld.any { it.name == "Specific Age Branch" })
    }

    @Test
    fun `hidden branches should not be returned for matching`() {
        val hiddenBranch = Branch().apply {
            name = "Hidden Branch"
            email = "hidden@example.com"
            minimumAge = 6
            maximumAge = 8
            sex = Sex.UNKNOWN
            description = "Hidden"
            law = "Law"
            image = "test.jpg"
            status = BranchStatus.HIDDEN
            staffTitle = "Leader"
        }

        branchRepository.save(hiddenBranch)
        branchRepository.flush()

        val matching = branchRepository.getPossibleBranchesForSexAndAge(Sex.MALE, 7)

        assertFalse(matching.any { it.name == "Hidden Branch" })
    }
}
