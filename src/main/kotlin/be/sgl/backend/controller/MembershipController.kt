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
    fun getAllMembershipsForCurrentUser(): ResponseEntity<List<MembershipDTO>> {
        // TODO
        return ResponseEntity.ok(emptyList())
    }

    @GetMapping("/current")
    @OnlyAuthenticated
    @Operation(summary = "Get the current membership for the current user")
    fun getCurrentMembershipsForCurrentUser(): ResponseEntity<List<MembershipDTO>> {
        // TODO
        return ResponseEntity.ok(emptyList())
    }

    @GetMapping("/user/{userId}/current")
    @OnlyStaff
    @Operation(summary = "Get the current memberships for the given user")
    fun getCurrentMembershipsForUser(@PathVariable userId: String): ResponseEntity<List<MembershipDTO>> {
        // TODO
        return ResponseEntity.ok(emptyList())
    }

    @GetMapping("/branch/{branchId}")
    @OnlyStaff
    fun getAllMembershipsForBranchAndCurrentPeriod(@PathVariable(required = false) branchId: Int?): ResponseEntity<List<MembershipDTO>> {
        // TODO
        return ResponseEntity.ok(emptyList())
    }

    @PostMapping
    @OnlyAuthenticated
    fun createMembershipForCurrentUserAndCurrentPeriod(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<Unit> {
        val headers = HttpHeaders()
        headers.location = URI("https://mysite.com/checkout")
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/user/{userId}")
    @OnlyStaff
    fun createMembershipForUserAndCurrentPeriod(@PathVariable userId: String): ResponseEntity<Unit> {
        val headers = HttpHeaders()
        headers.location = URI("https://mysite.com/checkout")
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
    @PreAuthorize("permitAll()")
    fun updatePayment(@RequestBody paymentId: String): ResponseEntity<Unit> {
        membershipService.updatePayment(paymentId)
        return ResponseEntity.ok().build()
    }
}