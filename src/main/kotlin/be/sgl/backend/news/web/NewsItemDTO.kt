package be.sgl.backend.news.web

import be.sgl.backend.news.domain.NewsItem
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(description = "NewsItem")
data class NewsItemDTO(
    val id: Int,
    val title: String,
    val content: String,
    val image: String?,
    val createdDate: LocalDateTime
) {
    companion object {
        fun of(item: NewsItem): NewsItemDTO {
            return NewsItemDTO(item.id!!, item.title, item.content, item.image, item.createdDate!!)
        }
    }
}
