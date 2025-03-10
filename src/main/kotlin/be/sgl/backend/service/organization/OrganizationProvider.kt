package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.Organization

interface OrganizationProvider {
    fun getOwner(): Organization
    fun getCertifier(): Organization
}