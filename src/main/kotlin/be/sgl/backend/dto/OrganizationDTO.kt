package be.sgl.backend.dto

import be.sgl.backend.entity.organization.ContactMethodType
import be.sgl.backend.entity.organization.OrganizationType
import be.sgl.backend.entity.user.User
import be.sgl.backend.util.Kbo
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.io.File

data class OrganizationDTO(
    var id: Int?,
    @NotBlank
    var name: String?,
    @NotNull
    var type: OrganizationType?,
    @Kbo
    var kbo: String?,
    @NotNull
    var address: AddressDTO?,
    var contactMethods: List<ContactMethodDTO>,
    var image: String?,
    var description: String?
)

data class ContactMethodDTO(
    var id: Int?,
    @NotNull
    var value: String?,
    @NotNull
    var type: ContactMethodType?
)

data class Representative(
    val user: User,
    val title: String,
    val signature: File
)

data class RepresentativeDTO(
    @field:NotBlank
    var username: String?,
    var title: String?,
    @field:NotBlank
    var signature: String?
)