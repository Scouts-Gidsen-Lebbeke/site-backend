package be.sgl.backend.entity.user

import be.sgl.backend.entity.Address
import java.time.LocalDate

object UserTestMother {

    fun user() = UserBuilder()

    class UserBuilder {
        private var id: Int? = 1
        private var username: String? = null
        private var externalId: String? = null
        private var customerId: String? = null
        private var memberId: String? = null
        private var name = "lastName"
        private var firstName = "firstName"
        private var email = "firstName.lastName@corp.com"
        private var birthdate = LocalDate.of(2004,1, 27)
        private var ageDeviation = 0
        private var sex = Sex.UNKNOWN
        private var image: String? = null
        private var mobile: String? = null
        private var nis: String? = null
        private var accountNo: String? = null
        private var hasReduction = false
        private var hasHandicap = false
        private val addresses = mutableListOf<Address>()
        private val contacts = mutableListOf<Contact>()
        private val roles = mutableListOf<UserRole>()
        private var staffData = StaffData()

        fun id(id: Int) = apply { this.id = id }

        fun username(username: String) = apply { this.username = username }

        fun externalId(externalId: String) = apply { this.externalId = externalId }

        fun customerId(customerId: String) = apply { this.customerId = customerId }

        fun memberId(memberId: String) = apply { this.memberId = memberId }

        fun name(name: String) = apply { this.name = name }

        fun firstName(firstName: String) = apply { this.firstName = firstName }

        fun email(email: String) = apply { this.email = email }

        fun birthdate(birthdate: LocalDate) = apply { this.birthdate = birthdate }

        fun ageDeviation(ageDeviation: Int) = apply { this.ageDeviation = ageDeviation }

        fun sex(sex: Sex) = apply { this.sex = sex }

        fun image(image: String) = apply { this.image = image }

        fun mobile(mobile: String) = apply { this.mobile = mobile }

        fun nis(nis: String) = apply { this.nis = nis }

        fun accountNo(accountNo: String) = apply { this.accountNo = accountNo }

        fun hasReduction() = apply { this.hasReduction = true }

        fun hasHandicap() = apply { this.hasHandicap = true }

        fun addresses(vararg addresses: Address) = apply { this.addresses.addAll(addresses) }

        fun contacts(vararg contacts: Contact) = apply { this.contacts.addAll(contacts) }

        fun roles(vararg roles: UserRole) = apply { this.roles.addAll(roles) }

        fun staffData(staffData: StaffData) = apply { this.staffData = staffData }

        fun build(): User {
            val user = User()
            user.id = id
            user.username = username
            user.externalId = externalId
            user.customerId = customerId
            user.memberId = memberId
            user.name = name
            user.firstName = firstName
            user.email = email
            user.birthdate = birthdate
            user.ageDeviation = ageDeviation
            user.sex = sex
            user.image = image
            user.mobile = mobile
            user.nis = nis
            user.accountNo = accountNo
            user.hasReduction = hasReduction
            user.hasHandicap = hasHandicap
            user.addresses.addAll(addresses)
            user.contacts.addAll(contacts)
            user.roles.addAll(roles)
            staffData.user = user
            user.staffData = staffData
            return user
        }
    }
}