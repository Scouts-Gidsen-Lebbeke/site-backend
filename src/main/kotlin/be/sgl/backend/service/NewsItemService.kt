package be.sgl.backend.service

import be.sgl.backend.dto.NewsItemDTO
import be.sgl.backend.entity.NewsItem
import be.sgl.backend.repository.NewsItemRepository
import be.sgl.backend.service.exception.NewsItemNotFoundException
import be.sgl.backend.mapper.NewsItemMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import be.sgl.backend.service.ImageService.ImageDirectory.*
import be.sgl.backend.util.nullIfBlank

@Service
class NewsItemService {

    @Autowired
    private lateinit var mapper: NewsItemMapper
    @Autowired
    private lateinit var newsItemRepository: NewsItemRepository
    @Autowired
    private lateinit var imageService: ImageService

    fun getVisibleItems(): List<NewsItemDTO> {
        return newsItemRepository.getNewsItemByVisibleTrue().map(mapper::toDto)
    }

    fun getNewsItemDTOById(id: Int): NewsItemDTO {
        return mapper.toDto(getNewsItemById(id))
    }

    fun saveNewsItemDTO(dto: NewsItemDTO): NewsItemDTO {
        val item = mapper.toEntity(dto)
        item.image = item.image.nullIfBlank()
        item.image?.let { imageService.move(it, TEMPORARY, NEWS_ITEMS) }
        return mapper.toDto(newsItemRepository.save(item))
    }

    fun mergeNewsItemDTOChanges(id: Int, dto: NewsItemDTO): NewsItemDTO {
        val item = getNewsItemById(id)
        item.title = dto.title
        item.content = dto.content
        if (item.image != dto.image.nullIfBlank()) {
            item.image?.let { imageService.delete(NEWS_ITEMS, it) }
            dto.image.nullIfBlank()?.let { imageService.move(it, TEMPORARY, NEWS_ITEMS) }
            item.image = dto.image.nullIfBlank()
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