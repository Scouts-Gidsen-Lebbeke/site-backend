package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.dto.RemoteFile
import be.sgl.backend.dto.UserDTO
import be.sgl.backend.service.user.UserService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "Endpoints for managing users.")
class UserController {

    @Autowired
    lateinit var userService: UserService

    @GetMapping("/profile")
    @OnlyAuthenticated
    @Operation(
        summary = "Get the current user",
        description = "Returns basic user data for the current user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDTO::class))]),
            ApiResponse(responseCode = "401", description = "User is not logged in", content = [Content(schema = Schema(hidden = true))]),
        ]
    )
    fun getProfile(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<UserDTO> {
        return ResponseEntity.ok(userService.getProfile(userDetails.username))
    }

    @PostMapping("/profile-picture")
    @Operation(
        summary = "Upload the profile picture to the current user",
        description = "Deletes the current profile picture if existing, uploads and links the new one.",
        responses = [
            ApiResponse(responseCode = "200", description = "Image uploaded", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
            ApiResponse(responseCode = "401", description = "User is not logged in"),
            ApiResponse(responseCode = "500", description = "Image error", content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))])
        ]
    )
    fun uploadProfilePicture(@RequestParam("file") file: MultipartFile, @AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<RemoteFile> {
        val uploadedPicture = userService.uploadProfilePicture(userDetails.username, file)
        return ResponseEntity.ok(RemoteFile(uploadedPicture))
    }

    @GetMapping("/{username}/profile")
    @OnlyAdmin
    @Operation(
        summary = "Get a specific user",
        description = "Returns basic user data for the user with the specified username.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = "application/json", schema = Schema(implementation = UserDTO::class))]),
            ApiResponse(responseCode = "401", description = "User has no staff role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getProfile(@PathVariable username: String): ResponseEntity<UserDTO> {
        return ResponseEntity.ok(userService.getProfile(username))
    }

    @GetMapping("/search")
    fun findUser(@RequestParam query: String): ResponseEntity<List<UserDTO>> {
        TODO()
    }
}