package be.sgl.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class NewsItemDTO(
    var id: Int? = null,
    @field:NotBlank(message = "{NotBlank.newsItem.title}")
    @field:Size(max = 50, message = "{Size.newsItem.title}")
    var title: String,
    @field:NotBlank(message = "{NotBlank.newsItem.content}")
    @field:Size(max = 1000, message = "{Size.newsItem.content}")
    var content: String,
    var image: String? = null,
    var createdDate: LocalDateTime?
)