package be.sgl.backend.dto.membership

import be.sgl.backend.dto.branch.BranchBaseDTO
import be.sgl.backend.dto.user.UserDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

// read-only
@Schema(name = "Membership")
data class MembershipDTO(
    val id: Int?,
    val period: MembershipPeriodBaseDTO,
    val branch: BranchBaseDTO,
    val user: UserDTO,
    val price: Double,
    val paid: Boolean,
    val createdDate: LocalDateTime?
)