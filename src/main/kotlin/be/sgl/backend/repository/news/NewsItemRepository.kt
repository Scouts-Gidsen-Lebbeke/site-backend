package be.sgl.backend.repository.news

import be.sgl.backend.entity.news.NewsItem
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface NewsItemRepository : JpaRepository<NewsItem, Int> {
    @Query("from NewsItem where visible order by createdDate desc")
    fun getNewsItemByVisibleTrue() : List<NewsItem>
}