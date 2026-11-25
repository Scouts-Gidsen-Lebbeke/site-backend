package be.sgl.backend.people.web

import be.sgl.backend.people.domain.ContactRole

object CreateOrUpdateContactRequestTestMother {

    fun contactRequest() = CreateOrUpdateContactRequestBuilder()

    class CreateOrUpdateContactRequestBuilder {
        private var userId: Int? = 1
        private var firstName: String? = "requestFirstName"
        private var name: String? = "requestName"
        private var role: ContactRole? = ContactRole.FATHER
        private var mobile: String? = "+32470000000"
        private var email: String? = "contact@corp.com"
        private var nis: String? = null
        private var addressId: Int? = null

        fun userId(userId: Int?) = apply { this.userId = userId }

        fun firstName(firstName: String?) = apply { this.firstName = firstName }

        fun name(name: String?) = apply { this.name = name }

        fun role(role: ContactRole?) = apply { this.role = role }

        fun mobile(mobile: String?) = apply { this.mobile = mobile }

        fun email(email: String?) = apply { this.email = email }

        fun nis(nis: String?) = apply { this.nis = nis }

        fun addressId(addressId: Int?) = apply { this.addressId = addressId }

        fun build() = CreateOrUpdateContactRequest(userId, firstName, name, role, mobile, email, nis, addressId)
    }
}
