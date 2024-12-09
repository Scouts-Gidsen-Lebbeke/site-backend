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
    lateinit var description: String

    /**
     * Start date at which payments can take place. Should always be before the [closed] date.
     */
    var open: LocalDateTime = LocalDateTime.now()

    /**
     * End date at which payments can take place. Should always be after the [open] date.
     */
    var closed: LocalDateTime = LocalDateTime.now()
}