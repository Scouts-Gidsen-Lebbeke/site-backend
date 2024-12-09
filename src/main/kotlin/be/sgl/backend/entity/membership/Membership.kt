package be.sgl.backend.entity.membership

import be.sgl.backend.entity.Auditable
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.user.User
import be.woutschoovaerts.mollie.data.payment.PaymentStatus
import jakarta.persistence.*

@Entity
@Table(
    indexes = [
        Index(name = "idx_payment_id", columnList = "payment_id", unique = true)
    ]
)
class Membership : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    @ManyToOne
    lateinit var user: User
    @ManyToOne
    lateinit var period: MembershipPeriod
    @ManyToOne
    lateinit var branch: Branch
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.OPEN
    var price: Double = 0.0
    var reductionFactor: Double = 3.0
    var paymentId: String? = null
    @OneToMany
    var restrictions = mutableListOf<MembershipRestriction>()
}