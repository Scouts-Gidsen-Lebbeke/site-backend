package be.sgl.backend.controller

import be.sgl.backend.config.security.LevelSecurityService
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.MembershipDTO
import be.sgl.backend.service.MembershipService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/memberships")
@Tag(name = "Memberships", description = "Endpoints for managing membership periods and creating memberships for users.")
class MembershipController {

    @Autowired
    private lateinit var levelSecurityService: LevelSecurityService
    @Autowired
    private lateinit var membershipService: MembershipService

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all memberships for the current user")
    fun getAllMembershipsForCurrentUser(): ResponseEntity<List<MembershipDTO>> {
        // TODO
        return ResponseEntity.ok(emptyList())
    }

    @GetMapping("/period/{periodId}")
    @OnlyStaff
    fun getAllMembershipsForCurrentPeriod(@PathVariable periodId: Int?): ResponseEntity<List<MembershipDTO>> {
        // TODO
        return ResponseEntity.ok(emptyList())
    }

    @PostMapping
    fun createMembershipForCurrentUserAndPeriod(@RequestParam branchId: Int): ResponseEntity<Unit> {
        val headers = HttpHeaders()
        headers.location = URI("https://mysite.com/checkout")
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/updatePayment")
    @PreAuthorize("permitAll()")
    fun updatePayment(@RequestParam id: String): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }
}