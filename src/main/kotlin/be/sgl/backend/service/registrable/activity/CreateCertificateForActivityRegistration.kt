package be.sgl.backend.service.registrable.activity

import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.service.organization.GetRepresentativeForOwner
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.util.*
import java.time.LocalDate

@Usecase
class CreateCertificateForActivityRegistration(
    private val organizationProvider: OrganizationProvider,
    private val getRepresentativeForOwner: GetRepresentativeForOwner
) {

    fun execute(registration: ActivityRegistration): ByteArray {
        check(registration.completed) { "A certificate can only be generated for a completed activity!" }
        val user = registration.user
        val owner = organizationProvider.getOwner()
        val representative = getRepresentativeForOwner.execute()
        val formData = mapOf(
            "name" to user.name,
            "first_name" to user.firstName,
            "birth_date" to user.birthdate.belgian(),
            "nis_nr" to user.nis,
            "address" to user.getHomeAddress(),
            "activity_name" to registration.subscribable.name,
            "period" to "${registration.start.belgian()} - ${registration.end.belgian()}",
            "days" to registration.calculateDays(),
            "amount" to "€ ${registration.price.pricePrecision()}",
            "payment_date" to registration.createdDate?.belgian(),
            "organization_name" to owner.name,
            "organization_address" to owner.address,
            "organization_email" to owner.getEmail(),
            "signature_date" to LocalDate.now().belgian(),
            "signatory" to representative.user.getFullName(),
            "id" to "${registration.subscribable.id}-#${registration.id}".base64Encoded()
        )
        return fillForm("forms/participation.pdf", formData, StampSpecs(representative.signature))
    }
}