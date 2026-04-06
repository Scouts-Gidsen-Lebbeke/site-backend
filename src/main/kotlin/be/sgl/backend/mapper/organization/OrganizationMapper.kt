package be.sgl.backend.mapper.organization

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.dto.organization.OrganizationDTO
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.organization.Organization
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface OrganizationMapper {
    fun toDto(organization: Organization): OrganizationDTO
    fun toEntity(dto: OrganizationDTO): Organization
    fun toEntity(dto: AddressDTO): Address
}