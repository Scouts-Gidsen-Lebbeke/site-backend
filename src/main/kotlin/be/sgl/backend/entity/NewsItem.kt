package be.sgl.backend.entity

import jakarta.persistence.*

@Entity
class NewsItem : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @Column(nullable = false, length = 50)
    lateinit var title: String
    @Column(nullable = false, length = 1000)
    lateinit var content: String
    var image: String? = null
    var visible: Boolean = true
}
