package be.sgl.backend.entity

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.LocalDateTime

@Entity
@Table(name = "news")
class NewsItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: User
    @NotBlank(message = "{NotBlank.newsItem.title}")
    @Size(max = 50, message = "{Size.newsItem.title}")
    var title: String = "new"
    @NotBlank(message = "{NotBlank.newsItem.content}")
    @Size(max = 500, message = "{Size.newsItem.content}")
    var content: String = ""
    var image: String? = null
    var visible: Boolean = true
    val date: LocalDateTime = LocalDateTime.now()
}
