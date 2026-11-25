package be.sgl.backend.organization.domain

import be.sgl.backend.shared.address.Address
import be.sgl.backend.shared.address.AddressTestMother
import org.springframework.test.util.ReflectionTestUtils

object OrganizationTestMother {

    fun organization() = OrganizationBuilder()

    class OrganizationBuilder {
        private var id: Int? = 1
        private var name = "organizationName"
        private var type = OrganizationType.OWNER
        private var kbo: String? = "0123456789"
        private var address = AddressTestMother.address().build()
        private val contactMethods = mutableListOf<Pair<ContactMethodType, String>>()
        private var image: String? = null
        private var description: String? = null

        fun id(id: Int?) = apply { this.id = id }

        fun name(name: String) = apply { this.name = name }

        fun owner() = apply { this.type = OrganizationType.OWNER }

        fun certifier() = apply { this.type = OrganizationType.CERTIFIER }

        fun type(type: OrganizationType) = apply { this.type = type }

        fun kbo(kbo: String?) = apply { this.kbo = kbo }

        fun address(address: Address) = apply { this.address = address }

        fun contactMethod(type: ContactMethodType, value: String) = apply { this.contactMethods.add(type to value) }

        fun email(email: String) = contactMethod(ContactMethodType.EMAIL, email)

        fun mobile(mobile: String) = contactMethod(ContactMethodType.MOBILE, mobile)

        fun image(image: String) = apply { this.image = image }

        fun description(description: String) = apply { this.description = description }

        fun build(): Organization {
            val organization = Organization()
            // id is a val on the entity, so it can only be set through reflection
            ReflectionTestUtils.setField(organization, "id", id)
            organization.name = name
            organization.type = type
            organization.kbo = kbo
            organization.address = address
            organization.image = image
            organization.description = description
            contactMethods.forEach { (type, value) ->
                organization.contactMethods.add(ContactMethod(organization, type, value))
            }
            return organization
        }
    }
}
