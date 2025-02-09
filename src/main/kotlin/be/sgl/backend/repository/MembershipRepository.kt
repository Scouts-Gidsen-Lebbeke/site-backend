package be.sgl.backend.repository

import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.user.User
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MembershipRepository : JpaRepository<Membership, Int> {
    fun getMembershipByPaymentId(paymentId: String): Membership?
    @Query(value = "from Membership where user = :user and now() between period.start and period.end")
    fun getCurrentByUser(user: User): Membership?
}