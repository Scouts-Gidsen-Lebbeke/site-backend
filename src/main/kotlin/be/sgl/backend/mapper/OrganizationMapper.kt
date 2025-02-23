package be.sgl.backend.mapper

import be.sgl.backend.dto.OrganizationDTO
import be.sgl.backend.entity.organization.Organization
import org.mapstruct.Mapper

@Mapper(componentModel = "spring")
interface OrganizationMapper {
    fun toDto(organization: Organization): OrganizationDTO
    fun toEntity(dto: OrganizationDTO): Organization
}