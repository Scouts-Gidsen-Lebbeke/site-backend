package be.sgl.backend.entity.activity

import be.sgl.backend.entity.Auditable
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob

@Entity
class Activity : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    @Lob
    lateinit var info: String
    @Lob
    var additionalInfo: String? = null
}