package be.sgl.backend.service.organization

import be.sgl.backend.dto.OrganizationDTO
import be.sgl.backend.dto.RepresentativeDTO
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.mapper.AddressMapper
import be.sgl.backend.mapper.OrganizationMapper
import be.sgl.backend.repository.AddressRepository
import be.sgl.backend.repository.OrganizationRepository
import be.sgl.backend.service.ImageService
import be.sgl.backend.service.ImageService.ImageDirectory.*
import be.sgl.backend.service.SettingService
import be.sgl.backend.service.exception.OrganizationNotFoundException
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.nullIfBlank
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service

@Service
class OrganizationService {

    @Autowired
    private lateinit var userDataProvider: UserDataProvider

    @Autowired
    protected lateinit var organizationRepository: OrganizationRepository
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider
    @Autowired
    private lateinit var mapper: OrganizationMapper
    @Autowired
    protected lateinit var addressRepository: AddressRepository
    @Autowired
    private lateinit var addressMapper: AddressMapper
    @Autowired
    private lateinit var imageService: ImageService
    @Autowired
    private lateinit var settingService: SettingService

    fun getOwner(): OrganizationDTO {
        return mapper.toDto(organizationProvider.getOwner())
    }

    fun getCertifier(): OrganizationDTO {
        return mapper.toDto(organizationProvider.getCertifier())
    }

    fun saveOrganizationDTO(dto: OrganizationDTO): OrganizationDTO {
        val organization = mapper.toEntity(dto)
        check(organizationRepository.getByType(dto.type) == null) { "This organization already exists!" }
        organization.image = organization.image.nullIfBlank()
        organization.image?.let { imageService.move(it, TEMPORARY, ORGANIZATION) }
        return mapper.toDto(organizationRepository.save(organization))
    }

    fun mergeOrganizationDTOChanges(id: Int, dto: OrganizationDTO): OrganizationDTO {
        val organization = organizationRepository.findById(id).orElseThrow { OrganizationNotFoundException() }
        organization.name = dto.name
        organization.kbo = dto.kbo
        if (dto.address.id == null) {
            addressRepository.delete(organization.address)
            organization.address = addressMapper.toEntity(dto.address)
        }
        if (organization.image != dto.image.nullIfBlank()) {
            organization.image?.let { imageService.delete(ORGANIZATION, it) }
            dto.image.nullIfBlank()?.let { imageService.move(it, TEMPORARY, ORGANIZATION) }
            organization.image = dto.image.nullIfBlank()
        }
        organization.description = dto.description
        return mapper.toDto(organizationRepository.save(organization))
    }

    fun getRepresentativeDTO(): RepresentativeDTO {
        return RepresentativeDTO(
            settingService.get(SettingId.REPRESENTATIVE_USERNAME),
            settingService.get(SettingId.REPRESENTATIVE_TITLE),
            settingService.get(SettingId.REPRESENTATIVE_SIGNATURE),
        )
    }

    @CacheEvict(cacheNames = ["representative"])
    fun mergeRepresentativeDTOChanges(dto: RepresentativeDTO): RepresentativeDTO {
        checkNotNull(userDataProvider.userExists(dto.username)) { "No valid username provided!" }
        imageService.move(dto.signature!!, TEMPORARY, ORGANIZATION)
        settingService.update(SettingId.REPRESENTATIVE_USERNAME, dto.username)
        settingService.update(SettingId.REPRESENTATIVE_TITLE, dto.title)
        settingService.update(SettingId.REPRESENTATIVE_SIGNATURE, dto.signature)
        return dto
    }
}