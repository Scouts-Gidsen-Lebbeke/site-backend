package be.sgl.backend.service.organization

import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserRegistration

interface OrganizationProvider {
    fun getOwner(): Organization
    fun getCertifier(): Organization
}