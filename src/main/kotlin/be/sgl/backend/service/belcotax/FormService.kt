package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormDTO
import be.sgl.backend.entity.organization.Organization
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.service.SettingService
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.belgian
import be.sgl.backend.util.fillForm
import be.sgl.backend.util.pricePrecision
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class FormService {

    @Autowired
    private lateinit var organizationProvider: OrganizationProvider

    fun createForm(form: DeclarationFormDTO): ByteArray {
        val owner = organizationProvider.getOwner()
        val representative = organizationProvider.getRepresentative()
        val certifier = organizationProvider.getCertifier()
        val formData = mapOf(
            "instanceName" to owner.name,
            "instanceKBO" to owner.kbo,
            "instanceStreet" to owner.address.street,
            "instanceNr" to "${owner.address.number}${owner.address.subPremise}",
            "instanceZip" to owner.address.zipcode,
            "instanceTown" to owner.address.town,
            "certifierName" to certifier.name,
            "certifierKBO" to certifier.kbo,
            "certifierStreet" to certifier.address.street,
            "certifierNr" to "${certifier.address.number}${certifier.address.subPremise}",
            "certifierZip" to certifier.address.zipcode,
            "certifierTown" to certifier.address.town,
            "name" to form.user.name,
            "firstName" to form.user.firstName,
            "birthDate" to form.user.birthdate,
            "street" to form.parent.address?.street,
            "nr" to "${form.parent.address?.number}${form.parent.address?.subPremise}",
            "zip" to form.parent.address?.zipcode,
            "town" to form.parent.address?.town,
            "nis" to form.parent.nis,
            "debtorName" to form.parent.name,
            "debtorFirstName" to form.parent.firstName,
            "debtorStreet" to form.parent.address?.street,
            "debtorNr" to "${form.parent.address?.number}${form.parent.address?.subPremise}",
            "debtorZip" to form.parent.address?.zipcode,
            "debtorTown" to form.parent.address?.town,
            "debtorNis" to form.parent.nis,
            "id" to form.id,
            "taxYear" to form.year,
            "period1" to form.activity1.asPeriod(),
            "period1Days" to form.activity1.calculateDays(),
            "period1Rate" to form.dailyPrice(form.activity1).pricePrecision(),
            "period1Price" to form.activity1.price.pricePrecision(),
            "period1" to form.activity1.asPeriod(),
            "period1Days" to form.activity1.calculateDays(),
            "period1Rate" to form.dailyPrice(form.activity1).pricePrecision(),
            "period1Price" to form.activity1.price.pricePrecision(),
            "period2" to form.activity2.asPeriod(),
            "period2Days" to form.activity2?.calculateDays(),
            "period2Rate" to form.dailyPrice(form.activity2).pricePrecision(),
            "period2Price" to form.activity2?.price.pricePrecision(),
            "period3" to form.activity2.asPeriod(),
            "period3Days" to form.activity3?.calculateDays(),
            "period3Rate" to form.dailyPrice(form.activity3).pricePrecision(),
            "period3Price" to form.activity3?.price.pricePrecision(),
            "period4" to form.activity3.asPeriod(),
            "period4Days" to form.activity4?.calculateDays(),
            "period4Rate" to form.dailyPrice(form.activity4).pricePrecision(),
            "period4Price" to form.activity4?.price.pricePrecision(),
            "totalPrice" to form.totalPrice.pricePrecision(),
            "location" to owner.address.town,
            "authorizer" to representative.user.getFullName(),
            "authorizationRole" to representative.title
        )
        return fillForm("forms/form28186.pdf", formData)
    }

    private fun ActivityRegistration?.asPeriod(): String? {
        this ?: return null
        return "${start.belgian()} - ${end.belgian()}"
    }
}