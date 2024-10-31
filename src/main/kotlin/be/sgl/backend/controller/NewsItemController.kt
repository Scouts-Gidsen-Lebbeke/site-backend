package be.sgl.backend.controller

import be.sgl.backend.entity.NewsItem
import be.sgl.backend.repository.NewsItemRepository
import be.sgl.backend.repository.UserRepository
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/news")
class NewsItemController {

    @Autowired
    lateinit var newsItemRepository: NewsItemRepository
    @Autowired
    lateinit var userRepository: UserRepository

    @GetMapping
    fun getVisibleItems(): List<NewsItem> {
        return newsItemRepository.getNewsItemByVisibleTrue()
    }

    @GetMapping("/{id}")
    fun getNewsItems(@PathVariable id: Long): ResponseEntity<NewsItem?> {
        val newsItem = newsItemRepository.findById(id)
        if (newsItem.isPresent) {
            return ResponseEntity.ok(newsItem.get())
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    fun postNewsItem(@Valid @RequestBody newsItem: NewsItem, @AuthenticationPrincipal userDetails: UserDetails): ResponseEntity<NewsItem> {
        println(userDetails)
        newsItem.user = userRepository.getUserByExternalIdEquals(userDetails.username)
        return ResponseEntity(newsItemRepository.save(newsItem), HttpStatus.CREATED)
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    fun deleteNewsItem(@PathVariable id: Long): ResponseEntity<String> {
        val newsItem = newsItemRepository.findById(id)
        if (newsItem.isPresent) {
            val updated = newsItem.get()
            updated.visible = false
            newsItemRepository.save(updated)
            return ResponseEntity.ok("News item deleted successfully.")
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("News item not found.")
    }
}