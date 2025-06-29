package be.sgl.backend.entity

import jakarta.persistence.*

@Entity
class Address : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    var externalId: String? = null
    @Column(nullable = false)
    lateinit var street: String
    @Column(nullable = false)
    lateinit var number: String
    var subPremise: String? = null
    @Column(nullable = false)
    lateinit var zipcode: String
    @Column(nullable = false)
    lateinit var town: String
    @Column(nullable = false)
    lateinit var country: String
    var description: String? = null
    var postalAdress: Boolean = false

    fun getStreetAdress(): String {
        return "$street $number${subPremise ?: ""}"
    }

    override fun toString(): String {
        return "${getStreetAdress()}, $zipcode $town ($country)"
    }
}