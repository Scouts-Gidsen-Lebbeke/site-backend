package be.sgl.backend.controller

import be.sgl.backend.config.BadRequestResponse
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.NewsItemDTO
import be.sgl.backend.service.NewsItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/news")
@Tag(name = "News", description = "Endpoints for managing news items.")
class NewsItemController {

    @Autowired
    lateinit var newsItemService: NewsItemService

    @GetMapping
    @Operation(
        summary = "Get all visible news items",
        description = "Returns a list of all visible news items, ordered by most recent.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(type = "array", implementation = NewsItemDTO::class))])
        ]
    )
    fun getVisibleItems(): ResponseEntity<List<NewsItemDTO>> {
        return ResponseEntity.ok(newsItemService.getVisibleItems())
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a specific news item",
        description = "Returns the news item with the given id, regardless of its visibility.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(implementation = NewsItemDTO::class))])
        ]
    )
    fun getNewsItem(@PathVariable id: Int): ResponseEntity<NewsItemDTO> {
        return ResponseEntity.ok(newsItemService.getNewsItemDTOById(id))
    }

    @PostMapping
    @OnlyStaff
    @Operation(
        summary = "Create a new news item",
        description = "Creates a news item with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "News item created", content = [Content(mediaType = "application/json", schema = Schema(implementation = NewsItemDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad news item format", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no staff role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun createNewsItem(@Valid @RequestBody newsItem: NewsItemDTO): ResponseEntity<NewsItemDTO> {
        return ResponseEntity(newsItemService.saveNewsItemDTO(newsItem), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyStaff
    @Operation(
        summary = "Update an existing news item",
        description = "Updates a news item, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "News item updated", content = [Content(mediaType = "application/json", schema = Schema(implementation = NewsItemDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad news item format", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no staff role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun updateNewsItem(@PathVariable id: Int, @Valid @RequestBody newsItem: NewsItemDTO): ResponseEntity<NewsItemDTO> {
        return ResponseEntity.ok(newsItemService.mergeNewsItemDTOChanges(id, newsItem))
    }

    @DeleteMapping("/{id}")
    @OnlyStaff
    @Operation(
        summary = "Delete an existing news item",
        description = "Deletes a news item, identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "News item deleted", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "401", description = "User has no staff role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun deleteNewsItem(@PathVariable id: Int): ResponseEntity<String> {
        newsItemService.deleteNewsItem(id)
        return ResponseEntity.ok("News item deleted successfully.")
    }
}