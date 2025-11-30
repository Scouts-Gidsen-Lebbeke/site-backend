package be.sgl.backend.service

import be.sgl.backend.dto.news.CreateOrUpdateNewsItemRequest
import be.sgl.backend.dto.news.NewsItemDTO
import be.sgl.backend.entity.NewsItem
import be.sgl.backend.repository.NewsItemRepository
import be.sgl.backend.service.exception.NewsItemNotFoundException
import be.sgl.backend.mapper.NewsItemMapper
import org.springframework.stereotype.Service
import be.sgl.backend.service.ImageService.ImageDirectory.*
import be.sgl.backend.util.nullIfBlank

@Service
class NewsItemService(
    private val mapper: NewsItemMapper,
    private val newsItemRepository: NewsItemRepository,
    private val imageService: ImageService
) {

    fun getVisibleItems(): List<NewsItemDTO> {
        return newsItemRepository.getNewsItemByVisibleTrue().map(mapper::toDto)
    }

    fun getNewsItemDTOById(id: Int): NewsItemDTO {
        return mapper.toDto(getNewsItemById(id))
    }

    fun createNewsItem(request: CreateOrUpdateNewsItemRequest): NewsItemDTO {
        val item = NewsItem()
        request.title?.let { item.title = it }
        request.content?.let { item.content = it }
        item.image = request.image.nullIfBlank()
        item.image?.let { imageService.move(it, TEMPORARY, NEWS_ITEMS) }
        return mapper.toDto(newsItemRepository.save(item))
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
        return mapper.toDto(newsItemRepository.save(item))
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