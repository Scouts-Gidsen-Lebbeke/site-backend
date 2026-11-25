package be.sgl.backend.people.domain

import be.sgl.backend.shared.address.Address

object ContactTestMother {

    fun contact() = ContactBuilder()

    class ContactBuilder {
        private var id: Int? = 1
        private var user = UserTestMother.user().username("username").build()
        private var externalId: String? = null
        private var name = "contactName"
        private var firstName = "contactFirstName"
        private var role = ContactRole.MOTHER
        private var mobile: String? = null
        private var email: String? = null
        private var nis: String? = null
        private var address: Address? = null

        fun id(id: Int) = apply { this.id = id }

        fun user(user: User) = apply { this.user = user }

        fun externalId(externalId: String) = apply { this.externalId = externalId }

        fun name(name: String) = apply { this.name = name }

        fun firstName(firstName: String) = apply { this.firstName = firstName }

        fun role(role: ContactRole) = apply { this.role = role }

        fun mobile(mobile: String) = apply { this.mobile = mobile }

        fun email(email: String) = apply { this.email = email }

        fun nis(nis: String) = apply { this.nis = nis }

        fun address(address: Address) = apply { this.address = address }

        fun build(): Contact {
            val contact = Contact()
            contact.id = id
            contact.user = user
            contact.externalId = externalId
            contact.name = name
            contact.firstName = firstName
            contact.role = role
            contact.mobile = mobile
            contact.email = email
            contact.nis = nis
            contact.address = address
            return contact
        }
    }
}
