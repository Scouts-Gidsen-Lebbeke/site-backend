package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Payment
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    indexes = [
        Index(name = "idx_payment_id", columnList = "payment_id", unique = true)
    ]
)
class Membership() : Payment() {
    @ManyToOne
    lateinit var user: User
    @ManyToOne
    lateinit var period: MembershipPeriod
    @ManyToOne
    lateinit var branch: Branch

    constructor(user: User, period: MembershipPeriod, branch: Branch, price: Double) : this() {
        this.user = user
        this.period = period
        this.branch = branch
        this.price = price
    }

    override fun getDescription() = "Lidgeld $period"
}