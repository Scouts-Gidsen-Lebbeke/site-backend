package be.sgl.backend.mapper

import be.sgl.backend.dto.news.NewsItemDTO
import be.sgl.backend.entity.NewsItem
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface NewsItemMapper {
    fun toDto(item: NewsItem): NewsItemDTO
}