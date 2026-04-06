package be.sgl.backend.service.membership

import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.service.organization.GetRepresentativeForOwner
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.util.*
import java.time.LocalDate

@Usecase
class CreateCertificateForMembership(
    private val organizationProvider: OrganizationProvider,
    private val getRepresentativeForOwner: GetRepresentativeForOwner
) {

    fun execute(membership: Membership): ByteArray {
        check(membership.paid) { "A certificate can only be generated for a paid membership!" }
        val owner = organizationProvider.getOwner()
        val representative = getRepresentativeForOwner.execute()
        val formData = mapOf(
            "name" to membership.user.name,
            "first_name" to membership.user.firstName,
            "birth_date" to membership.user.birthdate.belgian(),
            "nis_nr" to membership.user.nis,
            "address" to membership.user.getHomeAddress(),
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
        return fillForm("forms/membership.pdf", formData, StampSpecs(representative.signature))
    }
}