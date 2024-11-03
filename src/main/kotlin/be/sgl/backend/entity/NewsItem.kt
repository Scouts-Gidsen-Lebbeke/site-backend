package be.sgl.backend.entity

import jakarta.persistence.*

@Entity
class NewsItem : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var title: String = ""
    var content: String = ""
    var image: String? = null
    var visible: Boolean = true
}
