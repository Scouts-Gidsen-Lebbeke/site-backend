package be.sgl.backend.repository

import be.sgl.backend.entity.membership.MembershipPeriod
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MembershipPeriodRepository : JpaRepository<MembershipPeriod, Int> {
    @Query("from MembershipPeriod where now() between :start and :end")
    fun getActivePeriod(): MembershipPeriod
}