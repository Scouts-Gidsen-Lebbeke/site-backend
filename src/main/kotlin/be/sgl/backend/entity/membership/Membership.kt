package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import jakarta.persistence.*

@Entity
@Table(
    indexes = [
        Index(name = "idx_payment_id", columnList = "payment_id", unique = true)
    ]
)
class Membership : Payment() {
    @ManyToOne
    lateinit var user: User
    @ManyToOne
    lateinit var period: MembershipPeriod
    @ManyToOne
    lateinit var branch: Branch
    var reductionFactor: Double = 3.0
    @OneToMany
    var restrictions = mutableListOf<MembershipRestriction>()

    override fun getDescription() = "Lidgeld $period"
}