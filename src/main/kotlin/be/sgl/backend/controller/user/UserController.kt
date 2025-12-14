package be.sgl.backend.controller.user

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.RemoteFile
import be.sgl.backend.dto.user.UserDTO
import be.sgl.backend.service.user.UserService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints for managing users.")
class UserController(
    val userService: UserService
) {

    @GetMapping("/profile", produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Get the current user",
        description = "Returns basic user data for the current user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = UserDTO::class))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ]
    )
    fun getProfile(@AuthenticationPrincipal userDetails: CustomUserDetails): UserDTO {
        return userService.getUserWithDetails(userDetails)
    }

    @PostMapping("/profile-picture", consumes = [MULTIPART_FORM_DATA_VALUE], produces = [APPLICATION_JSON_VALUE])
    @OnlyAuthenticated
    @Operation(
        summary = "Upload the profile picture to the current user",
        description = "Deletes the current profile picture if existing, uploads and links the new one.",
        responses = [
            ApiResponse(responseCode = "200", description = "Image uploaded", content = [Content(schema = Schema(implementation = RemoteFile::class))]),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun uploadProfilePicture(@RequestParam file: MultipartFile, @AuthenticationPrincipal userDetails: CustomUserDetails): RemoteFile {
        val uploadedPicture = userService.uploadProfilePicture(userDetails.username, file)
        return RemoteFile(uploadedPicture)
    }

    @GetMapping("/{username}/profile", produces = [APPLICATION_JSON_VALUE])
    @OnlyAdmin
    @Operation(
        summary = "Get a specific user",
        description = "Returns basic user data for the user with the specified username.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(schema = Schema(implementation = UserDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getProfile(@PathVariable username: String): UserDTO {
        return userService.getProfile(username)
    }

    @GetMapping("/search", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Find a user based on name and/or first name",
        description = "Returns a list of all matching users. Only users where the name or first name contains the query are listed.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(items = Schema(implementation = UserDTO::class)))])
        ]
    )
    fun findUser(@RequestParam query: String): List<UserDTO> {
        return userService.getByQuery(query)
    }

    @GetMapping("/{username}/siblings", produces = [APPLICATION_JSON_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Get all the siblings of a specific user",
        description = "Returns a list of users registered as sibling from the user with the specified username.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(array = ArraySchema(schema = Schema(type = "array", implementation = UserDTO::class)))]),
            ApiResponse(responseCode = "404", description = "User not found", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getSiblings(@PathVariable username: String): List<UserDTO> {
        return userService.getSiblings(username)
    }
}