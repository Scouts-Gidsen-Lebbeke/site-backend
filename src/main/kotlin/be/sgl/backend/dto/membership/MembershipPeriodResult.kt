package be.sgl.backend.dto.membership

import be.sgl.backend.entity.membership.MembershipPeriod
import java.time.LocalDate

// read-only
data class MembershipPeriodResult(
    val id: Int?,
    val start: LocalDate,
    val end: LocalDate,
    val registrationCount: Int,
    val totalPrice: Double
) {
    constructor(period: MembershipPeriod, memberships: List<Double>) :
            this(period.id, period.start, period.end, memberships.count(), memberships.sum())
}