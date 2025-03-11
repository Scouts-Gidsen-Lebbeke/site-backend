package be.sgl.backend.entity.organization

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Auditable
import jakarta.persistence.*

@Entity
@Table(
    indexes = [
        Index(name = "idx_type", columnList = "type", unique = true)
    ]
)
class Organization : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
    lateinit var name: String
    var type = OrganizationType.OWNER
    var kbo: String? = null
    @ManyToOne
    lateinit var address: Address
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "organization", cascade = [(CascadeType.ALL)])
    val contactMethods: MutableList<ContactMethod> = mutableListOf()
    var image: String? = null
    @Column(length = 1000)
    var description: String? = null

    fun getEmail(): String? {
        return contactMethods.firstOrNull { it.type == ContactMethodType.EMAIL }?.value
    }

    fun getMobile(): String? {
        return contactMethods.firstOrNull { it.type == ContactMethodType.MOBILE }?.value
    }
}