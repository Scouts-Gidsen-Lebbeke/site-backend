package be.sgl.backend.dto

import be.sgl.backend.util.CountryCode
import jakarta.validation.constraints.NotNull
import java.io.Serializable

class AddressDTO(
    var id: Int?,
    @NotNull
    var street: String?,
    @NotNull
    var number: String?,
    var subPremise: String?,
    @NotNull
    var zipcode: String?,
    @NotNull
    var town: String?,
    @NotNull
    @CountryCode
    var country: String?,
    var description: String?,
    var postalAdress: Boolean
) : Serializable