package be.sgl.backend.controller

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.util.IntegrationTest
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@IntegrationTest
@AutoConfigureMockMvc
@Import(TestConfigurations::class)
@Transactional
class BranchControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var branchRepository: BranchRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var activeBranch: Branch
    private lateinit var passiveBranch: Branch

    @BeforeEach
    fun setup() {
        branchRepository.deleteAll()

        activeBranch = Branch().apply {
            name = "Active Branch"
            email = "active@example.com"
            minimumAge = 6
            maximumAge = 12
            sex = Sex.UNKNOWN
            description = "Active Description"
            law = "Law"
            image = "active.jpg"
            status = BranchStatus.ACTIVE
            staffTitle = "Leader"
        }
        activeBranch = branchRepository.save(activeBranch)

        passiveBranch = Branch().apply {
            name = "Passive Branch"
            email = "passive@example.com"
            minimumAge = 12
            maximumAge = 16
            sex = Sex.UNKNOWN
            description = "Passive Description"
            law = "Law"
            image = "passive.jpg"
            status = BranchStatus.PASSIVE
            staffTitle = "Leader"
        }
        passiveBranch = branchRepository.save(passiveBranch)
        branchRepository.flush()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    fun `GET all branches should return all branches for admin`() {
        mockMvc.perform(get("/branches"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET all branches should fail without authentication`() {
        mockMvc.perform(get("/branches"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `GET all branches should fail for regular user`() {
        mockMvc.perform(get("/branches"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET visible branches should return only non-passive branches`() {
        mockMvc.perform(get("/branches/visible"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[?(@.name == 'Active Branch')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Passive Branch')]").doesNotExist())
    }

    @Test
    fun `GET branches with calendar should return only active branches`() {
        mockMvc.perform(get("/branches/with-calendar"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[?(@.name == 'Active Branch')]").exists())
    }

    @Test
    fun `GET branch by id should return branch`() {
        mockMvc.perform(get("/branches/${activeBranch.id}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Active Branch"))
            .andExpect(jsonPath("$.email").value("active@example.com"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `GET branch by id should return 404 for non-existent branch`() {
        mockMvc.perform(get("/branches/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `POST branch should create new branch`() {
        val newBranch = mapOf(
            "name" to "New Branch",
            "email" to "new@example.com",
            "minimumAge" to 16,
            "maximumAge" to 18,
            "sex" to "UNKNOWN",
            "description" to "New Description",
            "law" to "Law",
            "status" to "ACTIVE",
            "staffTitle" to "Leader"
        )

        mockMvc.perform(
            post("/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBranch))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("New Branch"))
            .andExpect(jsonPath("$.email").value("new@example.com"))
    }

    @Test
    fun `POST branch should fail without authentication`() {
        val newBranch = mapOf(
            "name" to "New Branch",
            "email" to "new@example.com",
            "minimumAge" to 16,
            "maximumAge" to 18,
            "sex" to "UNKNOWN",
            "description" to "New Description",
            "law" to "Law",
            "status" to "ACTIVE",
            "staffTitle" to "Leader"
        )

        mockMvc.perform(
            post("/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newBranch))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `PUT branch should update existing branch`() {
        val updatedBranch = mapOf(
            "id" to activeBranch.id,
            "name" to "Updated Branch",
            "email" to "updated@example.com",
            "minimumAge" to activeBranch.minimumAge,
            "maximumAge" to activeBranch.maximumAge,
            "sex" to activeBranch.sex.toString(),
            "description" to "Updated Description",
            "law" to activeBranch.law,
            "status" to activeBranch.status.toString(),
            "staffTitle" to activeBranch.staffTitle
        )

        mockMvc.perform(
            put("/branches/${activeBranch.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBranch))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Updated Branch"))
            .andExpect(jsonPath("$.email").value("updated@example.com"))
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `PUT branch should return 404 for non-existent branch`() {
        val updatedBranch = mapOf(
            "id" to 999,
            "name" to "Updated Branch",
            "email" to "updated@example.com",
            "minimumAge" to 6,
            "maximumAge" to 12,
            "sex" to "UNKNOWN",
            "description" to "Updated Description",
            "law" to "Law",
            "status" to "ACTIVE",
            "staffTitle" to "Leader"
        )

        mockMvc.perform(
            put("/branches/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBranch))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT branch should fail without authentication`() {
        val updatedBranch = mapOf(
            "id" to activeBranch.id,
            "name" to "Updated Branch",
            "email" to "updated@example.com",
            "minimumAge" to activeBranch.minimumAge,
            "maximumAge" to activeBranch.maximumAge,
            "sex" to activeBranch.sex.toString(),
            "description" to "Updated Description",
            "law" to activeBranch.law,
            "status" to activeBranch.status.toString(),
            "staffTitle" to activeBranch.staffTitle
        )

        mockMvc.perform(
            put("/branches/${activeBranch.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedBranch))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET visible branches should be publicly accessible`() {
        mockMvc.perform(get("/branches/visible"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET branch by id should be publicly accessible`() {
        mockMvc.perform(get("/branches/${activeBranch.id}"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET branches with calendar should be publicly accessible`() {
        mockMvc.perform(get("/branches/with-calendar"))
            .andExpect(status().isOk)
    }
}
