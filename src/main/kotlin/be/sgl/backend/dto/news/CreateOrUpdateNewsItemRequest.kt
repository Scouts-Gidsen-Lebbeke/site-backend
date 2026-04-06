package be.sgl.backend.dto.news

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateOrUpdateNewsItemRequest(
    @field:NotBlank(message = "{NotBlank.newsItem.title}")
    @field:Size(max = 50, message = "{Size.newsItem.title}")
    var title: String?,
    @field:NotBlank(message = "{NotBlank.newsItem.content}")
    @field:Size(max = 1000, message = "{Size.newsItem.content}")
    var content: String?,
    var image: String?
)