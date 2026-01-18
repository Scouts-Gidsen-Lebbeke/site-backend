package be.sgl.backend.controller.user

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.dto.user.ContactDTO
import be.sgl.backend.dto.user.CreateOrUpdateContactRequest
import be.sgl.backend.service.user.ContactService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/contacts")
@Tag(name = "Contacts", description = "Endpoints for managing contacts.")
class ContactController(
    private val contactService: ContactService
) {

    @GetMapping("/user/{userId}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Get the contacts of the current user",
        description = "Returns a list of contacts for the current user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(implementation = ContactDTO::class)))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ]
    )
    fun getContacts(@PathVariable userId: Int, @AuthenticationPrincipal userDetails: CustomUserDetails): List<ContactDTO> {
        return contactService.getContactsForUser(userId, userDetails.username)
    }

    @GetMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Get a specific contact",
        description = "Returns the contact with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ]
    )
    fun getContact(@PathVariable id: Int, @AuthenticationPrincipal userDetails: CustomUserDetails): ContactDTO {
        return contactService.getContactDTOById(id, userDetails.username)
    }

    @PostMapping(consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Create a new contact",
        description = "Creates a contact with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Contact created"),
            ApiResponse(responseCode = "400", description = "Bad contact format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createContact(@Valid @RequestBody request: CreateOrUpdateContactRequest, @AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ContactDTO> {
        return ResponseEntity(contactService.createContact(request, userDetails.username), HttpStatus.CREATED)
    }

    @PutMapping("/{id}", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Update an existing contact",
        description = "Updates a contact, identified with the given id, with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "200", description = "Contact updated"),
            ApiResponse(responseCode = "400", description = "Bad contact format", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updateContact(@PathVariable id: Int, @Valid @RequestBody request: CreateOrUpdateContactRequest, @AuthenticationPrincipal userDetails: CustomUserDetails): ContactDTO {
        return contactService.updateContact(id, request, userDetails.username)
    }

    @DeleteMapping("/{id}", produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Delete an existing contact",
        description = "Deletes a contact, identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Contact deleted"),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun deleteContact(@PathVariable id: Int, @AuthenticationPrincipal userDetails: CustomUserDetails) {
        contactService.deleteContact(id, userDetails.username)
    }
}