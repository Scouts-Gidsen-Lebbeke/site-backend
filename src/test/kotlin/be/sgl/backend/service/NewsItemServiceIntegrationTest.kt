package be.sgl.backend.service

import be.sgl.backend.config.TestConfigurations
import be.sgl.backend.entity.NewsItem
import be.sgl.backend.repository.NewsItemRepository
import be.sgl.backend.service.exception.NewsItemNotFoundException
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
class NewsItemServiceIntegrationTest {

    @Autowired
    private lateinit var newsItemService: NewsItemService

    @Autowired
    private lateinit var newsItemRepository: NewsItemRepository

    @BeforeEach
    fun setup() {
        newsItemRepository.deleteAll()
    }

    @Test
    fun `getVisibleItems should only return visible news items`() {
        val visibleItem = NewsItem().apply {
            title = "Visible News"
            content = "This is visible"
            visible = true
        }

        val hiddenItem = NewsItem().apply {
            title = "Hidden News"
            content = "This is hidden"
            visible = false
        }

        newsItemRepository.save(visibleItem)
        newsItemRepository.save(hiddenItem)
        newsItemRepository.flush()

        val items = newsItemService.getVisibleItems()

        assertTrue(items.any { it.title == "Visible News" })
        assertFalse(items.any { it.title == "Hidden News" })
    }

    @Test
    fun `getNewsItemDTOById should return item when exists`() {
        val newsItem = NewsItem().apply {
            title = "Test News"
            content = "Test content"
            visible = true
        }
        val saved = newsItemRepository.save(newsItem)
        newsItemRepository.flush()

        val result = newsItemService.getNewsItemDTOById(saved.id!!)

        assertNotNull(result)
        assertEquals("Test News", result.title)
        assertEquals("Test content", result.content)
    }

    @Test
    fun `getNewsItemDTOById should throw exception when not found`() {
        assertThrows(NewsItemNotFoundException::class.java) {
            newsItemService.getNewsItemDTOById(999)
        }
    }

    @Test
    fun `deleteNewsItem should remove news item`() {
        val newsItem = NewsItem().apply {
            title = "To Delete"
            content = "Will be deleted"
            visible = true
        }
        val saved = newsItemRepository.save(newsItem)
        newsItemRepository.flush()

        newsItemService.deleteNewsItem(saved.id!!)

        assertFalse(newsItemRepository.existsById(saved.id!!))
    }

    @Test
    fun `deleteNewsItem should throw exception when not found`() {
        assertThrows(NewsItemNotFoundException::class.java) {
            newsItemService.deleteNewsItem(999)
        }
    }

    @Test
    fun `news items should be ordered by creation date`() {
        val older = NewsItem().apply {
            title = "Older News"
            content = "Older content"
            visible = true
        }

        val newer = NewsItem().apply {
            title = "Newer News"
            content = "Newer content"
            visible = true
        }

        newsItemRepository.save(older)
        Thread.sleep(10) // Ensure different timestamps
        newsItemRepository.save(newer)
        newsItemRepository.flush()

        val items = newsItemService.getVisibleItems()

        assertTrue(items.size >= 2)
        // Newer items should come first
        val newerIndex = items.indexOfFirst { it.title == "Newer News" }
        val olderIndex = items.indexOfFirst { it.title == "Older News" }
        assertTrue(newerIndex < olderIndex)
    }
}
