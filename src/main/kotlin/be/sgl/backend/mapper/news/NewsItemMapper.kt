package be.sgl.backend.mapper.news

import be.sgl.backend.dto.news.NewsItemDTO
import be.sgl.backend.entity.news.NewsItem
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface NewsItemMapper {
    fun toDto(item: NewsItem): NewsItemDTO
}