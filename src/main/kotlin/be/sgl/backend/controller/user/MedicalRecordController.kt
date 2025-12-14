package be.sgl.backend.controller.user

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.user.MedicalRecordDTO
import be.sgl.backend.service.user.MedicalRecordService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/medical-records")
@Tag(name = "Users", description = "Endpoints for managing users.")
class MedicalRecordController(private val medicalRecordService: MedicalRecordService) {

    @GetMapping(produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Get the medical record of the current user",
        description = "Returns the medical record for the current user, if existing.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = MedicalRecordDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found"),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ]
    )
    fun getMedicalRecord(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<MedicalRecordDTO?> {
        val medicalRecord = medicalRecordService.getMedicalRecord(userDetails.username)
        return medicalRecord?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/{username}", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Get the medical record of a specific user",
        description = "Returns the medical record for the user with the specified username, if existing.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = MedicalRecordDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found"),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ]
    )
    fun getMedicalRecord(@PathVariable username: String): ResponseEntity<MedicalRecordDTO?> {
        val medicalRecord = medicalRecordService.getMedicalRecord(username)
        return medicalRecord?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }
}