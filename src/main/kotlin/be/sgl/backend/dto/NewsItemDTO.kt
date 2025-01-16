package be.sgl.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class NewsItemDTO(
    var id: Int? = null,
    @NotBlank(message = "{NotBlank.newsItem.title}")
    @Size(max = 50, message = "{Size.newsItem.title}")
    var title: String,
    @NotBlank(message = "{NotBlank.newsItem.content}")
    @Size(max = 500, message = "{Size.newsItem.content}")
    var content: String,
    var image: String? = null,
    var createdDate: LocalDateTime?
)