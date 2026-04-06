package be.sgl.backend.dto.user

import io.swagger.v3.oas.annotations.media.Schema

// read-only
@Schema(name = "Staff")
data class StaffDTO(
    val name: String,
    val firstName: String,
    val image: String?,
    val nickname: String?,
    val totem: String?,
)