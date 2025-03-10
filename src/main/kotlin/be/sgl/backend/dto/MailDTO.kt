package be.sgl.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class MailDTO(
    @Email
    @NotNull
    val from: String,
    @NotEmpty
    val to: List<@Email String>,
    @Email
    val cc: String?,
    @NotBlank
    val subject: String,
    @NotBlank
    val body: String
)