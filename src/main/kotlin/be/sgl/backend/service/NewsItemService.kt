package be.sgl.backend.service

import be.sgl.backend.dto.NewsItemDTO
import be.sgl.backend.entity.NewsItem
import be.sgl.backend.repository.NewsItemRepository
import be.sgl.backend.service.exception.NewsItemNotFoundException
import be.sgl.backend.mapper.NewsItemMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NewsItemService {
    @Autowired
    private lateinit var mapper: NewsItemMapper
    @Autowired
    private lateinit var newsItemRepository: NewsItemRepository

    fun getVisibleItems(): List<NewsItemDTO> {
        return newsItemRepository.getNewsItemByVisibleTrue().map(mapper::toDto)
    }

    fun getNewsItemDTOById(id: Int): NewsItemDTO {
        return mapper.toDto(getNewsItemById(id))
    }

    fun saveNewsItemDTO(dto: NewsItemDTO): NewsItemDTO {
        return mapper.toDto(newsItemRepository.save(mapper.toEntity(dto)))
    }

    fun mergeNewsItemDTOChanges(id: Int, dto: NewsItemDTO): NewsItemDTO {
        val item = getNewsItemById(id)
        item.title = dto.title
        item.content = dto.content
        item.image = dto.image
        return mapper.toDto(newsItemRepository.save(item))
    }

    fun deleteNewsItem(id: Int) {
        val newsItem = getNewsItemById(id)
        newsItem.visible = false
        newsItemRepository.save(newsItem)
    }

    private fun getNewsItemById(id: Int): NewsItem {
        return newsItemRepository.findById(id).orElseThrow { NewsItemNotFoundException() }
    }
}