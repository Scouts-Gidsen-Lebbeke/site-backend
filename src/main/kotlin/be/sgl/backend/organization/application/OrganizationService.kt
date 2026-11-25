package be.sgl.backend.organization.application

import be.sgl.backend.organization.web.OrganizationDTO
import be.sgl.backend.shared.address.AddressRepository
import be.sgl.backend.organization.domain.OrganizationRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.shared.image.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.shared.exception.OrganizationNotFoundException
import be.sgl.backend.shared.util.nullIfBlank
import org.springframework.stereotype.Service

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository,
    private val organizationProvider: OrganizationProvider,
    private val addressRepository: AddressRepository,
    private val imageService: ImageService
) {

    fun getOwner(): OrganizationDTO {
        return OrganizationDTO.of(organizationProvider.getOwner())
    }

    fun getCertifier(): OrganizationDTO {
        return OrganizationDTO.of(organizationProvider.getCertifier())
    }

    fun createOrganization(dto: OrganizationDTO): OrganizationDTO {
        val organization = dto.toEntity()
        check(organizationRepository.getByType(dto.type!!) == null) { "This organization already exists!" }
        organization.image = organization.image.nullIfBlank()
        organization.image?.let { imageService.move(it, TEMPORARY, ORGANIZATION) }
        return OrganizationDTO.of(organizationRepository.save(organization))
    }

    fun updateOrganization(id: Int, dto: OrganizationDTO): OrganizationDTO {
        val organization = organizationRepository.findById(id).orElseThrow { OrganizationNotFoundException() }
        organization.name = dto.name!!
        organization.kbo = dto.kbo
        if (dto.address!!.id == null) {
            addressRepository.delete(organization.address)
            organization.address = dto.address!!.toEntity()
        }
        if (organization.image != dto.image.nullIfBlank()) {
            organization.image?.let { imageService.delete(ORGANIZATION, it) }
            dto.image.nullIfBlank()?.let { imageService.move(it, TEMPORARY, ORGANIZATION) }
            organization.image = dto.image.nullIfBlank()
        }
        organization.description = dto.description
        organization.contactMethods.clear()
        organization.contactMethods.addAll(dto.contactMethods.map { it.toEntity(organization) })
        return OrganizationDTO.of(organizationRepository.save(organization))
    }
}