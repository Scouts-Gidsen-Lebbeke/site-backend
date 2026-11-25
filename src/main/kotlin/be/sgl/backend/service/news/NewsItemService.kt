package be.sgl.backend.service.news

import be.sgl.backend.dto.news.CreateOrUpdateNewsItemRequest
import be.sgl.backend.dto.news.NewsItemDTO
import be.sgl.backend.entity.news.NewsItem
import be.sgl.backend.repository.news.NewsItemRepository
import be.sgl.backend.shared.exception.NewsItemNotFoundException
import be.sgl.backend.shared.image.ImageService
import org.springframework.stereotype.Service
import be.sgl.backend.shared.image.ImageService.ImageDirectory.*
import be.sgl.backend.shared.util.nullIfBlank

@Service
class NewsItemService(
    private val newsItemRepository: NewsItemRepository,
    private val imageService: ImageService
) {

    fun getVisibleItems(): List<NewsItemDTO> {
        return newsItemRepository.getNewsItemByVisibleTrue().map(NewsItemDTO::of)
    }

    fun getNewsItemDTOById(id: Int): NewsItemDTO {
        return NewsItemDTO.of(getNewsItemById(id))
    }

    fun createNewsItem(request: CreateOrUpdateNewsItemRequest): NewsItemDTO {
        val item = NewsItem()
        request.title?.let { item.title = it }
        request.content?.let { item.content = it }
        item.image = request.image.nullIfBlank()
        item.image?.let { imageService.move(it, TEMPORARY, NEWS_ITEMS) }
        return NewsItemDTO.of(newsItemRepository.save(item))
    }

    fun updateNewsItem(id: Int, request: CreateOrUpdateNewsItemRequest): NewsItemDTO {
        val item = getNewsItemById(id)
        request.title?.let { item.title = it }
        request.content?.let { item.content = it }
        if (item.image != request.image.nullIfBlank()) {
            item.image?.let { imageService.delete(NEWS_ITEMS, it) }
            request.image.nullIfBlank()?.let { imageService.move(it, TEMPORARY, NEWS_ITEMS) }
            item.image = request.image.nullIfBlank()
        }
        return NewsItemDTO.of(newsItemRepository.save(item))
    }

    fun deleteNewsItem(id: Int) {
        val item = getNewsItemById(id)
        item.image?.let { imageService.delete(NEWS_ITEMS, it) }
        newsItemRepository.delete(item)
    }

    private fun getNewsItemById(id: Int): NewsItem {
        return newsItemRepository.findById(id).orElseThrow { NewsItemNotFoundException() }
    }
}