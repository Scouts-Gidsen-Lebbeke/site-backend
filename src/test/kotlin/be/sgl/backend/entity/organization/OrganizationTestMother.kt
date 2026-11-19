package be.sgl.backend.entity.organization

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.AddressTestMother

object OrganizationTestMother {

    fun organization() = OrganizationBuilder()

    class OrganizationBuilder {
        private var name = "organizationName"
        private var type = OrganizationType.OWNER
        private var kbo: String? = "0123456789"
        private var address = AddressTestMother.address().build()
        private val contactMethods = mutableListOf<Pair<ContactMethodType, String>>()
        private var image: String? = null
        private var description: String? = null

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
