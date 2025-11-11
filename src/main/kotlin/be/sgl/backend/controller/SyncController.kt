package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.service.SseService
import be.sgl.backend.service.user.sync.ExternalMember
import be.sgl.backend.service.user.sync.SyncService
import be.sgl.backend.util.ForExternalOrganization
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@ForExternalOrganization
@RequestMapping("/sync")
class SyncController {

    @Autowired
    private lateinit var sseService: SseService
    @Autowired
    private lateinit var syncService: SyncService
    @Value("\${organization.external.id}")
    lateinit var externalOrganizationId: String

    @PutMapping("/users")
    @OnlyAdmin
    @Operation(
        summary = "Fetch all external data for all currently known users, and optionally create new users for unknown external ones",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = TEXT_PLAIN_VALUE)])
        ]
    )
    fun fetchExternalDataForAllUsers(): String {
        return sseService.schedule(syncService::syncUsers)
    }

    @PutMapping("/user/{username}")
    @OnlyAdmin
    @Operation(
        summary = "Fetch all external data for a currently known user, identified with the given username",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok")
        ]
    )
    fun fetchExternalDataForUser(@PathVariable username: String) {
        syncService.syncUser(username)
    }

    @PostMapping("/members")
    @OnlyAdmin
    @Operation(
        summary = "Synchronize the external state of all members",
        description = "Compares the internal role and membership state with the externally known users and their assigned functions, and synchronize missing and obsolete links.",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = TEXT_PLAIN_VALUE)])
        ]
    )
    fun syncMembers(): String {
        return sseService.schedule(syncService::syncMembers)
    }

    @GetMapping("/members")
    @OnlyAdmin
    @Operation(
        summary = "Get all out-of-sync members",
        description = "Compares the internal role and membership state with the externally known users and their assigned functions, and return the non-matching members.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = ExternalMember::class))]),
        ]
    )
    fun getUnsyncedMembers(): ResponseEntity<List<ExternalMember>> {
        return ResponseEntity.ok(syncService.getUnsyncedMembers())
    }

    @GetMapping("/external-id")
    @Public
    @Operation(
        summary = "Retrieve the configured external organization id",
        description = "Returns the organization id used for external synchronization. This endpoint is only reachable if the id is indeed present.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = TEXT_PLAIN_VALUE)])
        ]
    )
    fun getExternalId(): ResponseEntity<String> {
        return ResponseEntity.ok(externalOrganizationId)
    }

    @PostMapping("/member/{userId}/open-external-registration/{requestId}")
    @OnlyAdmin
    fun syncMemberWithOpenExternalRegistration(@PathVariable userId: Int, @PathVariable requestId: String): ResponseEntity<Unit> {
        if (syncService.syncMemberWithExternalOpenRegistration(userId, requestId)) {
            return ResponseEntity.noContent().build()
        }
        return ResponseEntity.badRequest().build()
    }

    @PostMapping("/member/{userId}/new-external-member-id")
    @OnlyAdmin
    fun synMemberWithNewExternalMemberId(@PathVariable userId: Int): ResponseEntity<Unit> {
        if (syncService.synMemberWithNewExternalMemberId(userId)) {
            return ResponseEntity.noContent().build()
        }
        return ResponseEntity.badRequest().build()
    }

    @PostMapping("/member/{userId}/unmatched-external-functions")
    @OnlyAdmin
    @Operation(summary = "Match the external functions with the current internal roles")
    fun syncMemberWithUnmatchedExternalFunctions(@PathVariable userId: Int): ResponseEntity<Unit> {
        syncService.syncMemberWithUnmatchedExternalFunctions(userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/member/{userId}/no-active-membership")
    @OnlyAdmin
    @Operation(summary = "Remove all external roles")
    fun syncMemberWithNoActiveMembership(@RequestBody externalMember: ExternalMember): ResponseEntity<Unit> {
        syncService.syncMemberWithNoActiveMembership(externalMember)
        return ResponseEntity.noContent().build()
    }
}
