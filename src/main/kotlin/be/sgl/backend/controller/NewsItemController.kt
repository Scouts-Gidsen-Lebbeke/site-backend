package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.NewsItemDTO
import be.sgl.backend.service.NewsItemService
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
    fun getVisibleItems(): ResponseEntity<List<NewsItemDTO>> {
        return ResponseEntity.ok(newsItemService.getVisibleItems())
    }

    @GetMapping("/{id}")
    fun getNewsItem(@PathVariable id: Int): ResponseEntity<NewsItemDTO> {
        return ResponseEntity.ok(newsItemService.getNewsItemDTOById(id))
    }

    @PostMapping
    @OnlyStaff
    fun createNewsItem(@Valid @RequestBody newsItem: NewsItemDTO): ResponseEntity<NewsItemDTO> {
        return ResponseEntity(newsItemService.saveNewsItemDTO(newsItem), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyStaff
    fun updateNewsItem(@PathVariable id: Int, @Valid @RequestBody newsItem: NewsItemDTO): ResponseEntity<NewsItemDTO> {
        return ResponseEntity.ok(newsItemService.mergeNewsItemDTOChanges(id, newsItem))
    }

    @DeleteMapping("/{id}")
    @OnlyStaff
    fun deleteNewsItem(@PathVariable id: Int): ResponseEntity<String> {
        newsItemService.deleteNewsItem(id)
        return ResponseEntity.ok("News item deleted successfully.")
    }
}