package be.sgl.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
abstract class Subscribable : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    @Lob
    lateinit var description: String
    var start = LocalDateTime.now()
    var end = LocalDateTime.now()
    var openSubscription = LocalDateTime.now()
    var closedSubscription = LocalDateTime.now()
    var price = 0.0
}