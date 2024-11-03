package be.sgl.backend.repository

import be.sgl.backend.entity.NewsItem
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository

@Repository
interface NewsItemRepository : JpaRepository<NewsItem, Int> {
    fun getNewsItemByVisibleTrue() : List<NewsItem>
}