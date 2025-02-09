package be.sgl.backend.service.mapper

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.Address
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface AddressMapper {
    fun toDto(address: Address): AddressDTO
    fun toEntity(dto: AddressDTO): Address
}