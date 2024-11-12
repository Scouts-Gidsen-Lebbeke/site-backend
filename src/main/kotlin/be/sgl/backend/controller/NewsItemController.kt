package be.sgl.backend.controller

import be.sgl.backend.dto.NewsItemDTO
import be.sgl.backend.service.NewsItemService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/news")
class NewsItemController {

    @Autowired
    lateinit var newsItemService: NewsItemService

    @GetMapping
    fun getVisibleItems(): List<NewsItemDTO> {
        return newsItemService.getVisibleItems()
    }

    @GetMapping("/{id}")
    fun getNewsItems(@PathVariable id: Int): ResponseEntity<NewsItemDTO?> {
        return ResponseEntity.ok(newsItemService.getNewsItemDTOById(id))
    }

    @PostMapping
    fun createNewsItem(@Valid @RequestBody newsItem: NewsItemDTO): ResponseEntity<NewsItemDTO> {
        return ResponseEntity(newsItemService.saveNewsItemDTO(newsItem), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    fun updateNewsItem(@PathVariable id: Int, @Valid @RequestBody newsItem: NewsItemDTO): ResponseEntity<NewsItemDTO> {
        return ResponseEntity.ok(newsItemService.mergeNewsItemDTOChanges(id, newsItem))
    }

    @DeleteMapping("/{id}")
    fun deleteNewsItem(@PathVariable id: Int): ResponseEntity<String> {
        newsItemService.deleteNewsItem(id)
        return ResponseEntity.ok("News item deleted successfully.")
    }
}