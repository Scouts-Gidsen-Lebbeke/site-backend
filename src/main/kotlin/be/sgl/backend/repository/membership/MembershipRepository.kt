package be.sgl.backend.repository.membership

import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MembershipRepository : JpaRepository<Membership, Int>, PaymentRepository<Membership> {
    @Query(value = "from Membership where user = :user and now() between period.start and period.end")
    fun getCurrentPossiblyUnpaidByUser(user: User): Membership?
    @Query(value = "from Membership where paid and user = :user and now() between period.start and period.end")
    fun getCurrentByUser(user: User): Membership?
    @Query(value = "from Membership where paid and now() between period.start and period.end")
    fun getCurrent(): MutableList<Membership>
    @Query(value = "from Membership where paid and branch = :branch and now() between period.start and period.end")
    fun getCurrentByBranch(branch: Branch): MutableList<Membership>
    @Query(value = "select count(*) from Membership where paid and period = :period and branch = :branch")
    fun countByPeriodAndBranch(period: MembershipPeriod, branch: Branch): Int
    @Query(value = "select count(*) from Membership where paid and period = :period")
    fun countByPeriod(period: MembershipPeriod): Int
    @Query(value = "from Membership where paid and user = :user")
    fun getMembershipsByUser(user: User): MutableList<Membership>
    fun existsByPeriodAndUser(period: MembershipPeriod, user: User): Boolean
}