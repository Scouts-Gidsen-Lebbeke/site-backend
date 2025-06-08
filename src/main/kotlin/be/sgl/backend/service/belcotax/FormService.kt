package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormDTO
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.util.StampSpecs
import be.sgl.backend.util.belgian
import be.sgl.backend.util.fillForm
import be.sgl.backend.util.priceWithCurrency
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FormService {

    @Autowired
    private lateinit var organizationProvider: OrganizationProvider

    fun createForm(form: DeclarationFormDTO): ByteArray {
        val owner = organizationProvider.getOwner()
        val representative = organizationProvider.getRepresentative()
        val certifier = organizationProvider.getCertifier()
        val formData = mapOf(
            "organization_name" to owner.name,
            "organization_kbo" to owner.kbo,
            "organization_street" to owner.address.street,
            "organization_no" to "${owner.address.number}${owner.address.subPremise ?: ""}",
            "organization_zip" to owner.address.zipcode,
            "organization_town" to owner.address.town,
            "certifier_name" to certifier.name,
            "certifier_kbo" to certifier.kbo,
            "certifier_street" to certifier.address.street,
            "certifier_no" to "${certifier.address.number}${certifier.address.subPremise ?: ""}",
            "certifier_zip" to certifier.address.zipcode,
            "certifier_town" to certifier.address.town,
            "name" to form.user.name,
            "first_name" to form.user.firstName,
            "birth_date" to form.user.birthdate.belgian(),
            "street" to form.parent.address?.street,
            "no" to "${form.parent.address?.number}${form.parent.address?.subPremise ?: ""}",
            "zip" to form.parent.address?.zipcode,
            "town" to form.parent.address?.town,
            "nis_nr" to form.parent.nis,
            "debtor_name" to form.parent.name,
            "debtor_first_name" to form.parent.firstName,
            "debtor_street" to form.parent.address?.street,
            "debtor_no" to "${form.parent.address?.number}${form.parent.address?.subPremise ?: ""}",
            "debtor_zip" to form.parent.address?.zipcode,
            "debtor_town" to form.parent.address?.town,
            "debtor_nis_nr" to form.parent.nis,
            "id" to form.id,
            "year" to form.year,
            "period1" to form.activity1.asPeriod(),
            "period1_days" to form.activity1.calculateDays(),
            "period1_rate" to form.dailyPrice(form.activity1).priceWithCurrency(),
            "period1_price" to form.activity1.price.priceWithCurrency(),
            "period2" to form.activity2.asPeriod(),
            "period2_days" to form.activity2?.calculateDays(),
            "period2_rate" to form.dailyPrice(form.activity2).priceWithCurrency(),
            "period2_price" to form.activity2?.price.priceWithCurrency(),
            "period3" to form.activity3.asPeriod(),
            "period3_days" to form.activity3?.calculateDays(),
            "period3_rate" to form.dailyPrice(form.activity3).priceWithCurrency(),
            "period3_price" to form.activity3?.price.priceWithCurrency(),
            "period4" to form.activity4.asPeriod(),
            "period4_ays" to form.activity4?.calculateDays(), // not a typo :(
            "period4_rate" to form.dailyPrice(form.activity4).priceWithCurrency(),
            "period4_price" to form.activity4?.price.priceWithCurrency(),
            "total_price" to form.totalPrice.priceWithCurrency(),
            "location" to owner.address.town,
            "signatory" to representative.user.getFullName(),
            "signatory_role" to representative.title,
            "signature_date" to LocalDate.now().belgian()
        )
        return fillForm("forms/form28186.pdf", formData, StampSpecs(representative.signature, 2, 270f, 110f))
    }

    private fun ActivityRegistration?.asPeriod(): String? {
        this ?: return null
        return "${start.belgian()} - ${end.belgian()}"
    }
}