package be.sgl.backend.dto

import java.io.Serializable

class AddressDTO(
    val street: String,
    val number: Int,
    val subPremise: String?,
    val zipcode: String,
    val town: String,
    val country: String,
    val description: String?
) : Serializable