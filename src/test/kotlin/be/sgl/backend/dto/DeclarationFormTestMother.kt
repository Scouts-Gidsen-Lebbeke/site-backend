package be.sgl.backend.dto

import be.sgl.backend.entity.Address
import be.sgl.backend.entity.AddressTestMother
import be.sgl.backend.entity.PaymentTestMother
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.user.Contact
import be.sgl.backend.entity.user.ContactTestMother
import be.sgl.backend.entity.user.User
import be.sgl.backend.entity.user.UserTestMother
import java.time.LocalDate
import java.time.LocalDateTime

object DeclarationFormTestMother {

    fun declarationForm() = DeclarationFormBuilder()

    class DeclarationFormBuilder {
        private var user = UserTestMother.user()
            .name("Doe")
            .firstName("John")
            .birthdate(LocalDate.of(2015, 3, 15))
            .nis("15031512383")
            .build()
        private var address = AddressTestMother.address().postalAddress().build()
        private var parent = ContactTestMother.contact().user(user).build()
        private var activity1 = PaymentTestMother.activityRegistration()
            .user(user)
            .paid(true)
            .price(100.0)
            .start(LocalDateTime.of(2025, 7, 1, 10, 0))
            .end(LocalDateTime.of(2025, 7, 10, 12, 0))
            .build()
        private var activity2: ActivityRegistration? = null
        private var activity3: ActivityRegistration? = null
        private var activity4: ActivityRegistration? = null
        private var rate = 14.4
        private var index = 0

        fun user(user: User) = apply { this.user = user }

        fun address(address: Address) = apply { this.address = address }

        fun parent(parent: Contact) = apply { this.parent = parent }

        fun activity1(activity1: ActivityRegistration) = apply { this.activity1 = activity1 }

        fun activity2(activity2: ActivityRegistration) = apply { this.activity2 = activity2 }

        fun activity3(activity3: ActivityRegistration) = apply { this.activity3 = activity3 }

        fun activity4(activity4: ActivityRegistration) = apply { this.activity4 = activity4 }

        fun rate(rate: Double) = apply { this.rate = rate }

        fun index(index: Int) = apply { this.index = index }

        fun build() = DeclarationForm(user, address, parent, activity1, activity2, activity3, activity4, rate, index)
    }
}
