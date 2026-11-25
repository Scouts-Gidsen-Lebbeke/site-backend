package be.sgl.backend.shared.address

object AddressTestMother {

    fun address() = AddressBuilder()

    class AddressBuilder {
        private var externalId: String? = null
        private var street = "Kerkstraat"
        private var number = "1"
        private var subPremise: String? = null
        private var zipcode = "9280"
        private var town = "Lebbeke"
        private var country = "BE"
        private var description: String? = null
        private var postalAddress = false

        fun externalId(externalId: String) = apply { this.externalId = externalId }

        fun street(street: String) = apply { this.street = street }

        fun number(number: String) = apply { this.number = number }

        fun subPremise(subPremise: String) = apply { this.subPremise = subPremise }

        fun zipcode(zipcode: String) = apply { this.zipcode = zipcode }

        fun town(town: String) = apply { this.town = town }

        fun country(country: String) = apply { this.country = country }

        fun description(description: String) = apply { this.description = description }

        fun postalAddress() = apply { this.postalAddress = true }

        fun build(): Address {
            val address = Address()
            address.externalId = externalId
            address.street = street
            address.number = number
            address.subPremise = subPremise
            address.zipcode = zipcode
            address.town = town
            address.country = country
            address.description = description
            address.postalAdress = postalAddress
            return address
        }
    }
}
