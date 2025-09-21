package be.sgl.backend.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@MappedSuperclass
abstract class Payable : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
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

    /**
     * Is the payment cancellable by the user (when requested before the [closed] date)?
     */
    var cancellable: Boolean = true

    /**
     * Should a confirmation email be sent when a user paid?
     */
    var sendConfirmation: Boolean = true

    /**
     * Should a confirmation email be sent when a payment is marked as completed?
     */
    var sendCompleteConfirmation: Boolean = false

    /**
     * Optional email address put in cc at each communication to a registered user.
     */
    var communicationCC: String? = null

    /**
     * Is this payable cancelled by the admin?
     */
    var cancelled: Boolean = false
}