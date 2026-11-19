package be.sgl.backend.dto

import be.sgl.backend.entity.Address
import be.sgl.backend.util.CountryCode
import jakarta.validation.constraints.NotBlank
import java.io.Serializable

class AddressDTO(
    var id: Int?,
    @field:NotBlank(message = "{NotBlank.address.street}")
    var street: String?,
    @field:NotBlank(message = "{NotBlank.address.number}")
    var number: String?,
    var subPremise: String?,
    @field:NotBlank(message = "{NotBlank.address.zipcode}")
    var zipcode: String?,
    @field:NotBlank(message = "{NotBlank.address.town}")
    var town: String?,
    @field:NotBlank(message = "{NotBlank.address.country}")
    @field:CountryCode
    var country: String?,
    var description: String?,
    var postalAdress: Boolean? = false
) : Serializable {

    fun toEntity() = Address().also {
        it.street = street!!
        it.number = number!!
        it.subPremise = subPremise
        it.zipcode = zipcode!!
        it.town = town!!
        it.country = country!!
        it.description = description
        postalAdress?.let { postal -> it.postalAdress = postal }
    }

    companion object {
        fun of(address: Address): AddressDTO {
            return AddressDTO(address.id, address.street, address.number, address.subPremise, address.zipcode,
                address.town, address.country, address.description, address.postalAdress)
        }
    }
}
