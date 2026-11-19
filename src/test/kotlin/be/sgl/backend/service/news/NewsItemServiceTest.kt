package be.sgl.backend.service.news

import be.sgl.backend.dto.news.CreateOrUpdateNewsItemRequest
import be.sgl.backend.dto.news.NewsItemDTO
import be.sgl.backend.entity.news.NewsItem
import be.sgl.backend.entity.news.NewsItemTestMother.newsItem
import be.sgl.backend.exception.NewsItemNotFoundException
import be.sgl.backend.repository.news.NewsItemRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.NEWS_ITEMS
import be.sgl.backend.service.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.util.anyValue
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class NewsItemServiceTest {

    @Mock
    lateinit var newsItemRepository: NewsItemRepository
    @Mock
    lateinit var imageService: ImageService
    @InjectMocks
    lateinit var testee: NewsItemService

    private val id = 1

    @Test
    fun `should map the visible news items`() {
        // given
        val item = newsItem().build()
        given(newsItemRepository.getNewsItemByVisibleTrue()).willReturn(listOf(item))

        // when
        val result = testee.getVisibleItems()

        // then
        assertThat(result).containsExactly(NewsItemDTO.of(item))
    }

    @Test
    fun `should throw when the news item does not exist`() {
        // given
        given(newsItemRepository.findById(id)).willReturn(Optional.empty())

        // when / then
        assertThatThrownBy { testee.getNewsItemDTOById(id) }
            .isInstanceOf(NewsItemNotFoundException::class.java)
            .hasMessage("News item not found")
    }

    @Nested
    inner class CreateNewsItem {

        @Test
        fun `should save the item and move its image when it has one`() {
            // given
            val request = CreateOrUpdateNewsItemRequest("title", "content", "news.jpg")
            val captor = argumentCaptor<NewsItem>()
            given(newsItemRepository.save(anyValue<NewsItem>())).willReturn(newsItem().build())

            // when
            testee.createNewsItem(request)

            // then
            then(newsItemRepository).should().save(captureValue(captor))
            assertThat(captor.value.title).isEqualTo(request.title)
            assertThat(captor.value.content).isEqualTo(request.content)
            assertThat(captor.value.image).isEqualTo(request.image)
            then(imageService).should().move("news.jpg", TEMPORARY, NEWS_ITEMS)
        }

        @Test
        fun `should store no image when the image is blank`() {
            // given
            val request = CreateOrUpdateNewsItemRequest("title", "content", "  ")
            val captor = argumentCaptor<NewsItem>()
            given(newsItemRepository.save(anyValue<NewsItem>())).willReturn(newsItem().build())

            // when
            testee.createNewsItem(request)

            // then
            then(newsItemRepository).should().save(captureValue(captor))
            assertThat(captor.value.image).isNull()
            then(imageService).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class UpdateNewsItem {

        @Test
        fun `should throw when the news item does not exist`() {
            // given
            given(newsItemRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateNewsItem(id, CreateOrUpdateNewsItemRequest("title", "content", null)) }
                .isInstanceOf(NewsItemNotFoundException::class.java)
        }

        @Test
        fun `should update the title and content`() {
            // given
            val item = newsItem().title("old title").content("old content").build()
            val request = CreateOrUpdateNewsItemRequest("new title", "new content", null)
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))
            given(newsItemRepository.save(item)).willReturn(item)

            // when
            testee.updateNewsItem(id, request)

            // then
            assertThat(item.title).isEqualTo(request.title)
            assertThat(item.content).isEqualTo(request.content)
        }

        @Test
        fun `should keep the title and content when they are not given`() {
            // given
            val item = newsItem().title("old title").content("old content").build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))
            given(newsItemRepository.save(item)).willReturn(item)

            // when
            testee.updateNewsItem(id, CreateOrUpdateNewsItemRequest(null, null, null))

            // then
            assertThat(item.title).isEqualTo("old title")
            assertThat(item.content).isEqualTo("old content")
        }

        @Test
        fun `should replace the image when it changed`() {
            // given
            val item = newsItem().image("old.jpg").build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))
            given(newsItemRepository.save(item)).willReturn(item)

            // when
            testee.updateNewsItem(id, CreateOrUpdateNewsItemRequest(null, null, "new.jpg"))

            // then
            then(imageService).should().delete(NEWS_ITEMS, "old.jpg")
            then(imageService).should().move("new.jpg", TEMPORARY, NEWS_ITEMS)
            assertThat(item.image).isEqualTo("new.jpg")
        }

        @Test
        fun `should add the image when there was none`() {
            // given
            val item = newsItem().build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))
            given(newsItemRepository.save(item)).willReturn(item)

            // when
            testee.updateNewsItem(id, CreateOrUpdateNewsItemRequest(null, null, "new.jpg"))

            // then
            then(imageService).should().move("new.jpg", TEMPORARY, NEWS_ITEMS)
            then(imageService).shouldHaveNoMoreInteractions()
        }

        @Test
        fun `should delete the image when the new image is blank`() {
            // given
            val item = newsItem().image("old.jpg").build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))
            given(newsItemRepository.save(item)).willReturn(item)

            // when
            testee.updateNewsItem(id, CreateOrUpdateNewsItemRequest(null, null, ""))

            // then
            then(imageService).should().delete(NEWS_ITEMS, "old.jpg")
            then(imageService).shouldHaveNoMoreInteractions()
            assertThat(item.image).isNull()
        }

        @Test
        fun `should keep the image when it did not change`() {
            // given
            val item = newsItem().image("same.jpg").build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))
            given(newsItemRepository.save(item)).willReturn(item)

            // when
            testee.updateNewsItem(id, CreateOrUpdateNewsItemRequest(null, null, "same.jpg"))

            // then
            then(imageService).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class DeleteNewsItem {

        @Test
        fun `should delete the item and its image when it has one`() {
            // given
            val item = newsItem().image("news.jpg").build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))

            // when
            testee.deleteNewsItem(id)

            // then
            then(imageService).should().delete(NEWS_ITEMS, "news.jpg")
            then(newsItemRepository).should().delete(item)
        }

        @Test
        fun `should only delete the item when it has no image`() {
            // given
            val item = newsItem().build()
            given(newsItemRepository.findById(id)).willReturn(Optional.of(item))

            // when
            testee.deleteNewsItem(id)

            // then
            then(imageService).shouldHaveNoInteractions()
            then(newsItemRepository).should().delete(item)
        }
    }
}
