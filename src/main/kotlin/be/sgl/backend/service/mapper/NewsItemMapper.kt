package be.sgl.backend.service.mapper

import be.sgl.backend.dto.NewsItemDTO
import be.sgl.backend.entity.NewsItem
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface NewsItemMapper {
    fun toDto(item: NewsItem): NewsItemDTO
    fun toEntity(dto: NewsItemDTO): NewsItem
}