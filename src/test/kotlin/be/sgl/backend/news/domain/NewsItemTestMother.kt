package be.sgl.backend.news.domain

import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

object NewsItemTestMother {

    fun newsItem() = NewsItemBuilder()

    class NewsItemBuilder {
        private var id: Int? = 1
        private var title = "newsItemTitle"
        private var content = "newsItemContent"
        private var image: String? = null
        private var visible = true
        private var createdDate: LocalDateTime? = LocalDateTime.of(2025, 9, 1, 12, 0)

        fun id(id: Int?) = apply { this.id = id }

        fun title(title: String) = apply { this.title = title }

        fun content(content: String) = apply { this.content = content }

        fun image(image: String) = apply { this.image = image }

        fun hidden() = apply { this.visible = false }

        fun createdDate(createdDate: LocalDateTime?) = apply { this.createdDate = createdDate }

        fun build(): NewsItem {
            val item = NewsItem()
            // id is a val on the entity, so it can only be set through reflection
            ReflectionTestUtils.setField(item, "id", id)
            item.title = title
            item.content = content
            item.image = image
            item.visible = visible
            item.createdDate = createdDate
            return item
        }
    }
}
