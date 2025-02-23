package be.sgl.backend.service.organization

import be.sgl.backend.dto.OrganizationDTO
import be.sgl.backend.mapper.OrganizationMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OrganizationService {

    @Autowired
    private lateinit var organizationProvider: OrganizationProvider
    @Autowired
    private lateinit var mapper: OrganizationMapper

    fun getOwner(): OrganizationDTO {
        return mapper.toDto(organizationProvider.getOwner())
    }
}