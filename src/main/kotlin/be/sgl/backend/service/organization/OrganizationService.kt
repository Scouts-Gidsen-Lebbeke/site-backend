package be.sgl.backend.service.organization

import be.sgl.backend.dto.organization.OrganizationDTO
import be.sgl.backend.mapper.organization.OrganizationMapper
import be.sgl.backend.repository.AddressRepository
import be.sgl.backend.repository.organization.OrganizationRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.service.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.exception.OrganizationNotFoundException
import be.sgl.backend.util.nullIfBlank
import org.springframework.stereotype.Service

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val organizationProvider: OrganizationProvider,
    private val mapper: OrganizationMapper,
    private val addressRepository: AddressRepository,
    private val imageService: ImageService
) {

    fun getOwner(): OrganizationDTO {
        return mapper.toDto(organizationProvider.getOwner())
    }

    fun getCertifier(): OrganizationDTO {
        return mapper.toDto(organizationProvider.getCertifier())
    }

    fun createOrganization(dto: OrganizationDTO): OrganizationDTO {
        val organization = mapper.toEntity(dto)
        check(organizationRepository.getByType(dto.type!!) == null) { "This organization already exists!" }
        organization.image = organization.image.nullIfBlank()
        organization.image?.let { imageService.move(it, TEMPORARY, ORGANIZATION) }
        return mapper.toDto(organizationRepository.save(organization))
    }

    fun updateOrganization(id: Int, dto: OrganizationDTO): OrganizationDTO {
        val organization = organizationRepository.findById(id).orElseThrow { OrganizationNotFoundException() }
        organization.name = dto.name!!
        organization.kbo = dto.kbo
        if (dto.address!!.id == null) {
            addressRepository.delete(organization.address)
            organization.address = mapper.toEntity(dto.address!!)
        }
        if (organization.image != dto.image.nullIfBlank()) {
            organization.image?.let { imageService.delete(ORGANIZATION, it) }
            dto.image.nullIfBlank()?.let { imageService.move(it, TEMPORARY, ORGANIZATION) }
            organization.image = dto.image.nullIfBlank()
        }
        organization.description = dto.description
        return mapper.toDto(organizationRepository.save(organization))
    }
}