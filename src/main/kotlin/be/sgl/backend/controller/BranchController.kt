package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.BranchBaseDTO
import be.sgl.backend.dto.BranchDTO
import be.sgl.backend.service.BranchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/branches")
@Tag(name = "Branches", description = "Endpoints for managing branches.")
class BranchController {

    @Autowired
    lateinit var branchService: BranchService

    @GetMapping
    @Operation(summary = "Get all branches")
    fun getAllBranches(): ResponseEntity<List<BranchBaseDTO>> {
        return ResponseEntity.ok(branchService.getAllBranches())
    }

    @GetMapping("/visible")
    @Operation(summary = "Get all visible branches")
    fun getVisibleBranches(): ResponseEntity<List<BranchBaseDTO>> {
        return ResponseEntity.ok(branchService.getVisibleBranches())
    }

    @GetMapping("/with-calendar")
    @Operation(summary = "Get all branches with an activity calendar")
    fun getBranchesWithCalendar(): ResponseEntity<List<BranchBaseDTO>> {
        return ResponseEntity.ok(branchService.getBranchesWithCalendar())
    }

    @GetMapping("/{id}")
    fun getBranch(@PathVariable id: Int): ResponseEntity<BranchDTO> {
        return ResponseEntity.ok(branchService.getBranchDTOById(id))
    }

    @PostMapping
    @OnlyStaff
    fun createBranch(@Valid @RequestBody branch: BranchDTO): ResponseEntity<BranchDTO> {
        return ResponseEntity(branchService.saveBranchDTO(branch), HttpStatus.CREATED)
    }

    @PutMapping("/{id}")
    @OnlyStaff
    fun updateBranch(@PathVariable id: Int, @Valid @RequestBody branch: BranchDTO): ResponseEntity<BranchDTO> {
        return ResponseEntity.ok(branchService.mergeBranchDTOChanges(id, branch))
    }

    @DeleteMapping("/{id}")
    @OnlyStaff
    fun deactivateBranch(@PathVariable id: Int): ResponseEntity<Unit> {
        branchService.deactivateBranch(id)
        return ResponseEntity.ok().build()
    }
}