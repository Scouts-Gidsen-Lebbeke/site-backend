package be.sgl.backend.controller

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.NewsItem
import be.sgl.backend.repository.NewsItemRepository
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
class NewsItemControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var newsItemRepository: NewsItemRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var visibleNewsItem: NewsItem
    private lateinit var hiddenNewsItem: NewsItem

    @BeforeEach
    fun setup() {
        newsItemRepository.deleteAll()

        visibleNewsItem = NewsItem().apply {
            title = "Visible News"
            content = "This is visible content"
            visible = true
        }
        visibleNewsItem = newsItemRepository.save(visibleNewsItem)

        hiddenNewsItem = NewsItem().apply {
            title = "Hidden News"
            content = "This is hidden content"
            visible = false
        }
        hiddenNewsItem = newsItemRepository.save(hiddenNewsItem)
        newsItemRepository.flush()
    }

    @Test
    fun `GET visible news items should return only visible items`() {
        mockMvc.perform(get("/news"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$[?(@.title == 'Visible News')]").exists())
            .andExpect(jsonPath("$[?(@.title == 'Hidden News')]").doesNotExist())
    }

    @Test
    fun `GET visible news items should be publicly accessible`() {
        mockMvc.perform(get("/news"))
            .andExpect(status().isOk)
    }

    @Test
    fun `GET news item by id should return news item`() {
        mockMvc.perform(get("/news/${visibleNewsItem.id}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value("Visible News"))
            .andExpect(jsonPath("$.content").value("This is visible content"))
            .andExpect(jsonPath("$.visible").value(true))
    }

    @Test
    fun `GET news item by id should return hidden items too`() {
        mockMvc.perform(get("/news/${hiddenNewsItem.id}"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value("Hidden News"))
            .andExpect(jsonPath("$.visible").value(false))
    }

    @Test
    fun `GET news item by id should return 404 for non-existent item`() {
        mockMvc.perform(get("/news/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET news item by id should be publicly accessible`() {
        mockMvc.perform(get("/news/${visibleNewsItem.id}"))
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `POST news item should create new item`() {
        val newNewsItem = mapOf(
            "title" to "New News",
            "content" to "New content",
            "visible" to true
        )

        mockMvc.perform(
            post("/news")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newNewsItem))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value("New News"))
            .andExpect(jsonPath("$.content").value("New content"))
            .andExpect(jsonPath("$.visible").value(true))
    }

    @Test
    fun `POST news item should fail without authentication`() {
        val newNewsItem = mapOf(
            "title" to "New News",
            "content" to "New content",
            "visible" to true
        )

        mockMvc.perform(
            post("/news")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newNewsItem))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `POST news item should fail for regular user`() {
        val newNewsItem = mapOf(
            "title" to "New News",
            "content" to "New content",
            "visible" to true
        )

        mockMvc.perform(
            post("/news")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newNewsItem))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `PUT news item should update existing item`() {
        val updatedNewsItem = mapOf(
            "id" to visibleNewsItem.id,
            "title" to "Updated News",
            "content" to "Updated content",
            "visible" to false
        )

        mockMvc.perform(
            put("/news/${visibleNewsItem.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedNewsItem))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.title").value("Updated News"))
            .andExpect(jsonPath("$.content").value("Updated content"))
            .andExpect(jsonPath("$.visible").value(false))
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `PUT news item should return 404 for non-existent item`() {
        val updatedNewsItem = mapOf(
            "id" to 999,
            "title" to "Updated News",
            "content" to "Updated content",
            "visible" to true
        )

        mockMvc.perform(
            put("/news/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedNewsItem))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT news item should fail without authentication`() {
        val updatedNewsItem = mapOf(
            "id" to visibleNewsItem.id,
            "title" to "Updated News",
            "content" to "Updated content",
            "visible" to false
        )

        mockMvc.perform(
            put("/news/${visibleNewsItem.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedNewsItem))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `DELETE news item should delete existing item`() {
        mockMvc.perform(delete("/news/${visibleNewsItem.id}"))
            .andExpect(status().isOk)

        // Verify deletion
        mockMvc.perform(get("/news/${visibleNewsItem.id}"))
            .andExpect(status().isNotFound)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `DELETE news item should return 404 for non-existent item`() {
        mockMvc.perform(delete("/news/999"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE news item should fail without authentication`() {
        mockMvc.perform(delete("/news/${visibleNewsItem.id}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    fun `DELETE news item should fail for regular user`() {
        mockMvc.perform(delete("/news/${visibleNewsItem.id}"))
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `POST news item should validate required fields`() {
        val invalidNewsItem = mapOf(
            "title" to "",  // Empty title
            "content" to "Some content",
            "visible" to true
        )

        mockMvc.perform(
            post("/news")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidNewsItem))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `news items should be ordered by most recent first`() {
        // Create a newer news item
        Thread.sleep(10) // Ensure different timestamps
        val newerNewsItem = NewsItem().apply {
            title = "Newer News"
            content = "Newer content"
            visible = true
        }
        newsItemRepository.save(newerNewsItem)
        newsItemRepository.flush()

        mockMvc.perform(get("/news"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value("Newer News"))
    }

    @Test
    @WithMockUser(roles = ["STAFF"])
    fun `PUT news item can toggle visibility`() {
        val toggledNewsItem = mapOf(
            "id" to visibleNewsItem.id,
            "title" to visibleNewsItem.title,
            "content" to visibleNewsItem.content,
            "visible" to false  // Toggle from true to false
        )

        mockMvc.perform(
            put("/news/${visibleNewsItem.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toggledNewsItem))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.visible").value(false))
    }
}
