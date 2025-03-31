package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.config.security.Public
import be.sgl.backend.dto.MembershipDTO
import be.sgl.backend.dto.MembershipPeriodDTO
import be.sgl.backend.dto.PaymentUrl
import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.service.membership.MembershipService
import be.sgl.backend.service.membership.MembershipPeriodService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/memberships")
@Tag(name = "Memberships", description = "Endpoints for managing membership periods and creating memberships for users.")
class MembershipController {

    @Autowired
    private lateinit var membershipService: MembershipService
    @Autowired
    private lateinit var membershipPeriodService: MembershipPeriodService

    @GetMapping
    @OnlyAuthenticated
    @Operation(summary = "Get all memberships for the current user")
    fun getAllMembershipsForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<List<MembershipDTO>> {
        return ResponseEntity.ok(membershipService.getAllMembershipsForUser(userDetails.username))
    }

    @GetMapping("/current")
    @OnlyAuthenticated
    @Operation(summary = "Get the current membership for the current user")
    fun getCurrentMembershipsForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<MembershipDTO?> {
        val currentMembership = membershipService.getCurrentMembershipForUser(userDetails.username)
        return currentMembership?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/user/{username}/current")
    @OnlyStaff
    @Operation(summary = "Get the current membership for the given user")
    fun getCurrentMembershipForUser(@PathVariable username: String): ResponseEntity<MembershipDTO?> {
        val currentMembership = membershipService.getCurrentMembershipForUser(username)
        return currentMembership?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/branch/{branchId}", "/branch")
    @OnlyStaff
    fun getAllMembershipsForBranch(@PathVariable(required = false) branchId: Int?): ResponseEntity<List<MembershipDTO>> {
        return ResponseEntity.ok(membershipService.getCurrentMembershipsForBranch(branchId))
    }

    @GetMapping("/{id}")
    @Public
    fun getMembershipById(@PathVariable id: Int): ResponseEntity<MembershipDTO?> {
        val membership = membershipService.getMembershipDTOById(id)
        return membership?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }

    @PostMapping
    @OnlyAuthenticated
    fun createMembershipForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<PaymentUrl> {
        val url = membershipService.createMembershipForExistingUser(userDetails.username)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/user/{username}")
    @OnlyStaff
    fun createMembershipForUser(@PathVariable username: String): ResponseEntity<PaymentUrl> {
        val url = membershipService.createMembershipForExistingUser(username)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/register")
    @Public
    fun createMembershipForNewUser(@Valid @RequestBody userRegistrationDTO: UserRegistrationDTO): ResponseEntity<PaymentUrl> {
        val url = membershipService.createMembershipForNewUser(userRegistrationDTO)
        return ResponseEntity.ok(PaymentUrl(url))
    }

    @PostMapping("/updatePayment", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    @CrossOrigin(origins = ["*"])
    @Public
    fun updatePayment(@RequestParam id: String): ResponseEntity<Unit> {
        membershipService.updatePayment(id)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/certificate")
    @OnlyAuthenticated
    fun getCertificateForMembership(@PathVariable id: Int): ResponseEntity<ByteArray> {
        val form = membershipService.getCertificateForMembership(id)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(form)
    }

    @GetMapping("/period/current")
    @Public
    fun getCurrentMembershipPeriod(): ResponseEntity<MembershipPeriodDTO> {
        return ResponseEntity.ok(membershipPeriodService.getCurrentMembershipPeriod())
    }
}