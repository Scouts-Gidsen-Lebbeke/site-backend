package be.sgl.backend.dto.organization

import be.sgl.backend.entity.organization.ContactMethodType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(name = "ContactMethod")
data class ContactMethodDTO(
    var id: Int?,
    @field:NotNull
    var value: String?,
    @field:NotNull
    var type: ContactMethodType?
)