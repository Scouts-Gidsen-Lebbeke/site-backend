package be.sgl.backend.entity

import jakarta.persistence.*

@Entity
class NewsItem : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var title: String
    lateinit var content: String
    var image: String? = null
    var visible: Boolean = true
}
