package be.sgl.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class Payable : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    @Lob
    lateinit var info: String
    var open: LocalDateTime = LocalDateTime.now()
    var closed: LocalDateTime = LocalDateTime.now()
}