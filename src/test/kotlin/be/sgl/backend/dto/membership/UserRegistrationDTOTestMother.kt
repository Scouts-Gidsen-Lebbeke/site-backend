package be.sgl.backend.dto.membership

import be.sgl.backend.dto.AddressDTO
import be.sgl.backend.entity.AddressTestMother
import be.sgl.backend.entity.user.Sex
import java.time.LocalDate

object UserRegistrationDTOTestMother {

    fun userRegistration() = UserRegistrationDTOBuilder()

    class UserRegistrationDTOBuilder {
        private var name = "lastName"
        private var firstName = "firstName"
        private var birthdate: LocalDate? = LocalDate.of(2015, 3, 14)
        private var email = "parent@corp.com"
        private var mobile = "+32470123456"
        private var sex = Sex.FEMALE
        private var hasReduction = false
        private var address: AddressDTO? = AddressDTO.of(AddressTestMother.address().build())

        fun name(name: String) = apply { this.name = name }

        fun firstName(firstName: String) = apply { this.firstName = firstName }

        fun birthdate(birthdate: LocalDate?) = apply { this.birthdate = birthdate }

        fun email(email: String) = apply { this.email = email }

        fun mobile(mobile: String) = apply { this.mobile = mobile }

        fun sex(sex: Sex) = apply { this.sex = sex }

        fun hasReduction() = apply { this.hasReduction = true }

        fun address(address: AddressDTO?) = apply { this.address = address }

        fun build() = UserRegistrationDTO(name, firstName, birthdate, email, mobile, sex, hasReduction, address)
    }
}
