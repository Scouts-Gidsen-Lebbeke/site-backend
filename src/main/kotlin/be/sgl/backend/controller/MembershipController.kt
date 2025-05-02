package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.*
import be.sgl.backend.service.membership.MembershipService
import be.sgl.backend.service.membership.MembershipPeriodService
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/memberships")
@Tag(name = "Memberships", description = "Endpoints for managing membership periods and creating memberships for users.")
class MembershipController {

    @Autowired
    private lateinit var membershipPeriodService: MembershipPeriodService
    @Autowired
    private lateinit var membershipService: MembershipService

    @GetMapping("/periods")
    @Public
    @Operation(
        summary = "Get all membership periods",
        description = "Returns a list of all membership periods.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = MembershipPeriodDTO::class))])
        ]
    )
    fun getAllMembershipPeriods(): ResponseEntity<List<MembershipPeriodDTO>> {
        return ResponseEntity.ok(membershipPeriodService.getAllMembershipPeriods())
    }

    @GetMapping("/periods/{id}")
    @Public
    @Operation(
        summary = "Get a specific membership period",
        description = "Returns the membership period with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipPeriodDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getMembershipPeriod(@PathVariable id: Int): ResponseEntity<MembershipPeriodDTO> {
        return ResponseEntity.ok(membershipPeriodService.getMembershipPeriodDTOById(id))
    }

    @GetMapping("/periods/current")
    @Public
    @Operation(
        summary = "Get the current membership period",
        description = "Returns the currently active membership period.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipPeriodDTO::class))])
        ]
    )
    fun getCurrentMembershipPeriod(): ResponseEntity<MembershipPeriodDTO> {
        return ResponseEntity.ok(membershipPeriodService.getCurrentMembershipPeriod())
    }

    @PostMapping("/periods")
    @OnlyAdmin
    @Operation(
        summary = "Create a membership period",
        description = "Creates a membership period with the provided request body and returns it.",
        responses = [
            ApiResponse(responseCode = "201", description = "Membership period created", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad membership period format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createPeriod(@Valid @RequestBody membershipPeriodDTO: MembershipPeriodDTO): ResponseEntity<MembershipPeriodDTO> {
        return ResponseEntity(membershipPeriodService.saveMembershipPeriodDTO(membershipPeriodDTO), HttpStatus.CREATED)
    }

    @PutMapping("/periods/{id}")
    @OnlyAdmin
    @Operation(
        summary = "Update an existing membership period",
        description = "Updates a membership period, identified with the given id, with the provided request body and returns it. Only future periods can be edited.",
        responses = [
            ApiResponse(responseCode = "200", description = "Membership period updated", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipPeriodDTO::class))]),
            ApiResponse(responseCode = "400", description = "Bad membership period format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun updatePeriod(@PathVariable id: Int, @Valid @RequestBody membershipPeriodDTO: MembershipPeriodDTO): ResponseEntity<MembershipPeriodDTO> {
        return ResponseEntity.ok(membershipPeriodService.mergeMembershipPeriodDTOChanges(id, membershipPeriodDTO))
    }

    @GetMapping
    @OnlyAuthenticated
    @Operation(
        summary = "Get all memberships for the current user",
        description = "Returns a list of all valid (i.e. paid and not cancelled) memberships for the given user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = MembershipDTO::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getAllMembershipsForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<List<MembershipDTO>> {
        return ResponseEntity.ok(membershipService.getAllMembershipsForUser(userDetails.username))
    }

    @GetMapping("/current")
    @OnlyAuthenticated
    @Operation(
        summary = "Get the current membership for the current user",
        description = "Returns the paid membership linked to the current period for the current user.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCurrentMembershipsForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<MembershipDTO?> {
        val currentMembership = membershipService.getCurrentMembershipForUser(userDetails.username)
        return currentMembership?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/user/{username}/current")
    @OnlyStaff
    @Operation(
        summary = "Get the current membership for the given user",
        description = "Returns the paid membership linked to the current period for the user identified with the given username.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found")
        ]
    )
    fun getCurrentMembershipForUser(@PathVariable username: String): ResponseEntity<MembershipDTO?> {
        val currentMembership = membershipService.getCurrentMembershipForUser(username)
        return currentMembership?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/branch/{branchId}", "/branch")
    @OnlyStaff
    @Operation(
        summary = "Get all current memberships for a branch",
        description = "Returns the memberships linked to the current membership period. If a branch is provided, only the memberships for this branch are listed",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(type = "array", implementation = MembershipDTO::class))]),
        ]
    )
    fun getAllMembershipsForBranch(@PathVariable(required = false) branchId: Int?): ResponseEntity<List<MembershipDTO>> {
        return ResponseEntity.ok(membershipService.getCurrentMembershipsForBranch(branchId))
    }

    @GetMapping("/{id}")
    @Public
    @Operation(
        summary = "Get a specific membership",
        description = "Returns the membership identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = MembershipDTO::class))]),
            ApiResponse(responseCode = "204", description = "Not found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getMembershipById(@PathVariable id: Int): ResponseEntity<MembershipDTO?> {
        val membership = membershipService.getMembershipDTOById(id)
        return membership?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @PostMapping
    @OnlyAuthenticated
    @Operation(
        summary = "Register the current user to the current membership period",
        description = "Creates a membership for the current membership period and the current user (if allowed) and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createMembershipForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<PaymentUrl> {
        val url = membershipService.createMembershipForExistingUser(userDetails.username)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/user/{username}")
    @OnlyStaff
    @Operation(
        summary = "Register a user to the current membership period",
        description = "Creates a membership for the current membership period and the user identified with the given username (if allowed) and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createMembershipForUser(@PathVariable username: String): ResponseEntity<PaymentUrl> {
        val url = membershipService.createMembershipForExistingUser(username)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/register")
    @Public
    @Operation(
        summary = "Register a new user to the current membership period",
        description = "Creates a membership for the current membership period and a new user created with the given request body (if allowed) and returns the payment url.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = PaymentUrl::class))]),
            ApiResponse(responseCode = "400", description = "Bad registration format", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "400", description = "Registration isn't possible", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun createMembershipForNewUser(@Valid @RequestBody userRegistrationDTO: UserRegistrationDTO): ResponseEntity<PaymentUrl> {
        val url = membershipService.createMembershipForNewUser(userRegistrationDTO)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/updatePayment", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @CrossOrigin(origins = ["*"])
    @Public
    @Operation(
        summary = "Trigger a payment update request",
        description = "Retrieves the payment based on the provided id and updates the payment status of the linked membership. This call never fails (except on server errors), to avoid exposing payment data.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok")
        ]
    )
    fun updatePayment(@RequestParam id: String): ResponseEntity<Unit> {
        membershipService.updatePayment(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/certificate")
    @OnlyAuthenticated
    @Operation(
        summary = "Generate the membership certificate",
        description = "Returns a pdf with a membership certificate for the membership identified with the given id.",
        responses = [
            ApiResponse(responseCode = "200", description = "Ok", content = [Content(mediaType = APPLICATION_PDF_VALUE)]),
            ApiResponse(responseCode = "400", description = "Membership isn't paid", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Invalid id", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getCertificateForMembership(@PathVariable id: Int): ResponseEntity<ByteArray> {
        val form = membershipService.getCertificateForMembership(id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(form)
    }
}