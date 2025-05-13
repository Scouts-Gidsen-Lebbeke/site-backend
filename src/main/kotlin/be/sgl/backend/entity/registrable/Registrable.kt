package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Payable
import com.dashjoin.jsonata.Jsonata.jsonata
import com.fasterxml.jackson.databind.ObjectMapper
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

    /**
     * Is the registration cancellable by the user (when requested before the [closed] date)?
     */
    var cancellable: Boolean = true

    /**
     * Should a confirmation email be sent when a user paid the registration?
     */
    var sendConfirmation: Boolean = true

    /**
     * Should a confirmation email be sent when a registration is marked as completed?
     */
    var sendCompleteConfirmation: Boolean = false

    /**
     * Optional email address put in cc at each communication to a registered user.
     */
    var communicationCC: String? = null

    /**
     * Is this event cancelled by the admin?
     */
    var cancelled: Boolean = false

    fun readAdditionalData(additionalData: String?): Double {
        additionalFormRule ?: return 0.0
        additionalData ?: return 0.0
        val values = ObjectMapper().readerFor(Map::class.java)
            .readValue<Map<String, String>>(additionalData)
            .mapValues { it.value.toDoubleOrNull() ?: it.value }
        return jsonata(additionalFormRule)
            .evaluate(values)
            .toString().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}