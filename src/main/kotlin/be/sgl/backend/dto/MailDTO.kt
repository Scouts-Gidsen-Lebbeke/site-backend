package be.sgl.backend.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class MailDTO(
    @field:Email
    @field:NotNull
    var from: String?,
    @field:NotEmpty
    var to: List<@Email String>,
    @field:Email
    var cc: String?,
    @field:NotBlank
    var subject: String?,
    @field:NotBlank
    var body: String?
)