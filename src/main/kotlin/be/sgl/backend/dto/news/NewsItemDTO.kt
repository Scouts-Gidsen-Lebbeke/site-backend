package be.sgl.backend.dto.news

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
)