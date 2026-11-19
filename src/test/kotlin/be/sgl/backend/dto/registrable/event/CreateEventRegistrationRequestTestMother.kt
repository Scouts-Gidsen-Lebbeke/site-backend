package be.sgl.backend.dto.registrable.event

object CreateEventRegistrationRequestTestMother {

    fun eventRegistrationRequest() = CreateEventRegistrationRequestBuilder()

    class CreateEventRegistrationRequestBuilder {
        private var name = "lastName"
        private var firstName = "firstName"
        private var email = "email@domain.com"
        private var mobile: String? = null
        private var additionalData: String? = null

        fun name(name: String) = apply { this.name = name }

        fun firstName(firstName: String) = apply { this.firstName = firstName }

        fun email(email: String) = apply { this.email = email }

        fun mobile(mobile: String) = apply { this.mobile = mobile }

        fun additionalData(additionalData: String) = apply { this.additionalData = additionalData }

        fun build() = CreateEventRegistrationRequest(name, firstName, email, mobile, additionalData)
    }
}
