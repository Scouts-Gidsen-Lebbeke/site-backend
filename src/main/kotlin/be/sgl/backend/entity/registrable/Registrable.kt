package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Payable
import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class Registrable : Payable() {

    /**
     * Start of the physical event this registrable is representing.
     * Should always be after the [closed] date and before the [end] date.
     */
    var start: LocalDateTime = LocalDateTime.now()

    /**
     * End of the physical event this registrable is representing.
     * Should always be after the [start] date.
     */
    var end: LocalDateTime = LocalDateTime.now()
    var price = 0.0
    var registrationLimit: Int? = null
    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    var address: Address? = null
    @Lob
    var additionalForm: String? = null
    var additionalFormRule: String? = null
}