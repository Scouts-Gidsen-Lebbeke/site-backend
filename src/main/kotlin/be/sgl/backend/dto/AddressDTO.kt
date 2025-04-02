package be.sgl.backend.dto

import be.sgl.backend.util.CountryCode
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.io.Serializable

class AddressDTO(
    val id: Int?,
    val street: String,
    val number: String,
    val subPremise: String?,
    val zipcode: String,
    val town: String,
    @NotNull
    @CountryCode
    val country: String,
    val description: String?
) : Serializable