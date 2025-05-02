package be.sgl.backend.service.membership

import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

@Usecase
class CreateCertificateForMembership {

    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider

    fun execute(membership: Membership): ByteArray {
        check(membership.paid) { "A certificate can only be generated for a paid membership!" }
        val user = userDataProvider.getUser(membership.user.username!!)
        val owner = organizationProvider.getOwner()
        val representative = organizationProvider.getRepresentative()
        val formData = mapOf(
            "name" to user.name,
            "first_name" to user.firstName,
            "birth_date" to user.birthdate.belgian(),
            "nis_nr" to user.nis,
            "address" to user.getHomeAddress(),
            "membership_period" to membership.period,
            "amount" to "€ ${membership.price.pricePrecision()}",
            "payment_date" to membership.createdDate?.belgian(),
            "organization_name" to owner.name,
            "organization_address" to owner.address,
            "organization_email" to owner.getEmail(),
            "signature_date" to LocalDate.now().belgian(),
            "signatory" to representative.user.getFullName(),
            "id" to "${membership.period.id}-#${membership.id}".base64Encoded()
        )
        return fillForm("forms/membership.pdf", formData, representative.signature)
    }
}