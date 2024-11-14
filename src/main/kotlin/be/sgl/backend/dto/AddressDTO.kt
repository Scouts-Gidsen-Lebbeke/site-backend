package be.sgl.backend.dto

import java.io.Serializable

class AddressDTO(
    val street: String,
    val number: Int,
    val subPremise: String?,
    val zipcode: String,
    val city: String,
    val country: String
) : Serializable