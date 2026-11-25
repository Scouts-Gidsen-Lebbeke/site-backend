package be.sgl.backend.organization.web

import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.shared.util.IntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@IntegrationTest
class BranchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var branchRepository: BranchRepository

    @AfterEach
    fun cleanUp() {
        branchRepository.deleteAll()
    }

    @Test
    fun `should return branch without staff field when getting a branch`() {
        // given
        val branch = branchRepository.save(branch().id(null).name("Welpen").build())

        // when
        val result = mockMvc.perform(get("/branches/${branch.id}"))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value(branch.name))
            .andExpect(jsonPath("$.staff").doesNotExist())
    }

    @Test
    fun `should return 404 when branch id is unknown`() {
        // given
        val unknownId = Int.MAX_VALUE

        // when
        val result = mockMvc.perform(get("/branches/$unknownId"))

        // then
        result.andExpect(status().isNotFound)
    }
}
