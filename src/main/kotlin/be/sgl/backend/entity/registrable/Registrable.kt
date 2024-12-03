package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Payable
import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class Registrable : Payable() {
    var start: LocalDateTime = LocalDateTime.now()
    var end: LocalDateTime = LocalDateTime.now()
    var price = 0.0
    @OneToOne
    lateinit var address: Address
    @Lob
    var additionalForm: String? = null
    var additionalFormRule: String? = null
}