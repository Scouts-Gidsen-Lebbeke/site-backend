package be.sgl.backend.news.web

import be.sgl.backend.news.domain.NewsItemRepository
import be.sgl.backend.news.domain.NewsItemTestMother.newsItem
import be.sgl.backend.organization.domain.RoleRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.util.IntegrationTest
import be.sgl.backend.shared.util.PersistedUsers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@IntegrationTest
class NewsItemControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    @Autowired
    private lateinit var newsItemRepository: NewsItemRepository
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var roleRepository: RoleRepository

    private val users by lazy { PersistedUsers(userRepository, roleRepository) }
    private val request = """{"title": "Kamp", "content": "Inschrijvingen zijn open"}"""

    @AfterEach
    fun cleanUp() {
        newsItemRepository.deleteAll()
        users.deleteAll()
    }

    @Test
    fun `should only list visible items when some items are hidden`() {
        // given
        val visible = newsItemRepository.save(newsItem().id(null).title("Zichtbaar").build())
        newsItemRepository.save(newsItem().id(null).title("Verborgen").hidden().build())

        // when
        val result = mockMvc.perform(get("/news"))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value(visible.title))
    }

    @Test
    fun `should return not found when the news item does not exist`() {
        // given

        // when
        val result = mockMvc.perform(get("/news/$UNKNOWN_ID"))

        // then
        result.andExpect(status().isNotFound)
    }

    @Test
    fun `should forbid creating a news item when the user is not staff`() {
        // given
        users.withoutRoles(MEMBER)

        // when
        val result = mockMvc.perform(post("/news").contentType(APPLICATION_JSON).content(request).with(jwt().jwt { it.subject(MEMBER) }))

        // then
        result.andExpect(status().isForbidden)
        assertThat(newsItemRepository.count()).isZero()
    }

    @Test
    fun `should create the news item when staff posts one`() {
        // given
        users.admin(ADMIN)

        // when
        val result = mockMvc.perform(post("/news").contentType(APPLICATION_JSON).content(request).with(jwt().jwt { it.subject(ADMIN) }))

        // then
        result.andExpect(status().isCreated).andExpect(jsonPath("$.title").value("Kamp"))
        assertThat(newsItemRepository.findAll()).singleElement().extracting { it.title }.isEqualTo("Kamp")
    }

    companion object {
        private const val ADMIN = "admin"
        private const val MEMBER = "member"
        private const val UNKNOWN_ID = 999_999
    }
}
