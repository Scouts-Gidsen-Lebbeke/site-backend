package be.sgl.backend.dto

import be.sgl.backend.util.CountryCode
import jakarta.validation.constraints.NotNull
import java.io.Serializable

class AddressDTO(
    var id: Int?,
    var street: String,
    var number: String,
    var subPremise: String?,
    var zipcode: String,
    var town: String,
    @NotNull
    @CountryCode
    var country: String,
    var description: String?
) : Serializable