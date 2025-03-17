package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.MembershipDTO
import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.service.MembershipService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/memberships")
@Tag(name = "Memberships", description = "Endpoints for managing membership periods and creating memberships for users.")
class MembershipController {

    @Autowired
    private lateinit var membershipService: MembershipService

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
        return ResponseEntity.ok(membershipService.getCurrentMembershipForUser(userDetails.username))
    }

    @GetMapping("/user/{username}/current")
    @OnlyStaff
    @Operation(summary = "Get the current membership for the given user")
    fun getCurrentMembershipForUser(@PathVariable username: String): ResponseEntity<MembershipDTO?> {
        return ResponseEntity.ok(membershipService.getCurrentMembershipForUser(username))
    }

    @GetMapping("/branch/{branchId}")
    @OnlyStaff
    fun getAllMembershipsForBranch(@PathVariable(required = false) branchId: Int?): ResponseEntity<List<MembershipDTO>> {
        return ResponseEntity.ok(membershipService.getCurrentMembershipsForBranch(branchId))
    }

    @GetMapping("/{id}")
    @OnlyAuthenticated
    fun getMembershipById(@PathVariable id: Int): ResponseEntity<MembershipDTO> {
        return ResponseEntity.ok(membershipService.getMembershipDTOById(id))
    }

    @PostMapping
    @OnlyAuthenticated
    fun createMembershipForCurrentUser(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<Unit> {
        val headers = HttpHeaders()
        headers.location = URI(membershipService.createMembershipForExistingUser(userDetails.username))
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/user/{username}")
    @OnlyStaff
    fun createMembershipForUser(@PathVariable username: String): ResponseEntity<Unit> {
        val headers = HttpHeaders()
        headers.location = URI(membershipService.createMembershipForExistingUser(username))
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/register")
    @PreAuthorize("permitAll()")
    fun createMembershipForNewUser(@RequestBody userRegistrationDTO: UserRegistrationDTO): ResponseEntity<Unit> {
        val headers = HttpHeaders()
        headers.location = URI(membershipService.createMembershipForNewUser(userRegistrationDTO))
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/updatePayment")
    @CrossOrigin(origins = ["*"])
    @PreAuthorize("permitAll()")
    fun updatePayment(@RequestBody paymentId: String): ResponseEntity<Unit> {
        membershipService.updatePayment(paymentId)
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
}