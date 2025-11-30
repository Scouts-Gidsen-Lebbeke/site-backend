package be.sgl.backend.controller.news

import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.news.CreateOrUpdateNewsItemRequest
import be.sgl.backend.dto.news.NewsItemDTO
import be.sgl.backend.service.NewsItemService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RestController
@RequestMapping("/news")
@Tag(name = "News", description = "Endpoints for managing news items.")
class NewsItemController(
    private val newsItemService: NewsItemService
) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get all visible news items",
        description = "Returns a list of all visible news items, ordered by most recent.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = NewsItemDTO::class)))])
        ]
    )
    fun getVisibleItems(): List<NewsItemDTO> {
        return newsItemService.getVisibleItems()
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @Public
    @Operation(
        summary = "Get a specific news item",
        description = "Returns the news item with the given id, regardless of its visibility.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = NewsItemDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getNewsItem(@PathVariable id: Int): NewsItemDTO {
        return newsItemService.getNewsItemDTOById(id)
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Create a new news item",
        description = "Creates a news item with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "News item created", content = [Content(schema = Schema(implementation = NewsItemDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad news item format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createNewsItem(@Valid @RequestBody request: CreateOrUpdateNewsItemRequest): ResponseEntity<NewsItemDTO> {
        return ResponseEntity(newsItemService.createNewsItem(request), HttpStatus.CREATED)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Update an existing news item",
        description = "Updates a news item, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "News item updated", content = [Content(schema = Schema(implementation = NewsItemDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad news item format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateNewsItem(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateNewsItemRequest): NewsItemDTO {
        return newsItemService.updateNewsItem(id, request)
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Delete an existing news item",
        description = "Deletes a news item, identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "News item deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteNewsItem(@PathVariable id: Int) {
        newsItemService.deleteNewsItem(id)
    }
}