package be.sgl.backend.service.organization

import be.sgl.backend.dto.ExternalFunction
import be.sgl.backend.dto.Representative
import be.sgl.backend.entity.organization.Organization

interface OrganizationProvider {
    fun getOwner(): Organization
    fun getRepresentative(): Representative
    fun getCertifier(): Organization
    fun getAllExternalFunctions(): List<ExternalFunction>
    fun getPaidExternalFunctions(): List<ExternalFunction>
}