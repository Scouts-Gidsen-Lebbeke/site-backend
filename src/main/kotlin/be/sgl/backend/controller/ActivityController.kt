package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.ActivityDTO
import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.service.activity.ActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.net.URI

@RestController
@RequestMapping("/activities")
@Tag(name = "Activity", description = "Endpoints for creating activities and registering users for them.")
class ActivityController {

    @Autowired
    private lateinit var activityService: ActivityService

    @GetMapping
    @OnlyAdmin
    @Operation(summary = "Get all activities")
    fun getAllActivities(): ResponseEntity<List<ActivityDTO>> {
        return ResponseEntity.ok(activityService.getAllActivities())
    }

    @GetMapping("/visible")
    @Operation(summary = "Get all activities that are not ended.")
    fun getVisibleActivities(): ResponseEntity<List<ActivityDTO>> {
        return ResponseEntity.ok(activityService.getVisibleActivities())
    }

    @GetMapping("/{id}")
    fun getActivity(@PathVariable id: Int): ResponseEntity<ActivityDTO> {
        return ResponseEntity.ok(activityService.getActivityDTOById(id))
    }

    @PostMapping
    @OnlyAdmin
    fun createActivity(@Valid @RequestBody branch: ActivityDTO): ResponseEntity<ActivityDTO> {
        return ResponseEntity(activityService.saveActivityDTO(branch), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyAdmin
    fun updateActivity(@PathVariable id: Int, @Valid @RequestBody activity: ActivityDTO): ResponseEntity<ActivityDTO> {
        return ResponseEntity.ok(activityService.mergeActivityDTOChanges(id, activity))
    }

    @DeleteMapping("/{id}")
    @OnlyAdmin
    fun deleteActivity(@PathVariable id: Int): ResponseEntity<String> {
        activityService.deleteActivity(id)
        return ResponseEntity.ok("Activity deactivated successfully.")
    }

    @PostMapping("/{id}/register/{restrictionId}")
    fun registerCurrentUser(@PathVariable id: Int, @PathVariable restrictionId: Int, @AuthenticationPrincipal userDetails: CustomUserDetails,
                            @RequestBody data: String, request: HttpServletRequest): ResponseEntity<Unit> {
        request.requestURI + "/updatePayment"
        val checkoutUrl = activityService.createPaymentForActivity(id, restrictionId, userDetails.username, data)
        val headers = HttpHeaders()
        headers.location = URI(checkoutUrl)
        return ResponseEntity(headers, HttpStatus.FOUND)
    }

    @PostMapping("/{id}/register/{restrictionId}/updatePayment")
    @PreAuthorize("permitAll()")
    fun updatePayment(@PathVariable id: Int, @PathVariable restrictionId: Int): ResponseEntity<Unit> {
        return ResponseEntity.ok().build()
    }

    @GetMapping("/{id}/registrations")
    @OnlyStaff
    @Operation(summary = "Get all registrations for the given activity.")
    fun getAllRegistrationsForActivity(@PathVariable id: Int): ResponseEntity<List<ActivityRegistrationDTO>> {
        return ResponseEntity.ok(activityService.getAllRegistrationsForActivity(id))
    }
}