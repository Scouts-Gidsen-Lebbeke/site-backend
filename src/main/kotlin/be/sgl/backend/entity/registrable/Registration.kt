package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.Payment
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class Registration<T : Registrable> : Payment() {
    @ManyToOne
    lateinit var subscribable: T
    var present = false
    @Lob
    var additionalData: String? = null
}