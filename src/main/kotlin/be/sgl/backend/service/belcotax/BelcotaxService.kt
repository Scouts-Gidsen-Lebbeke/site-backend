package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormDTO
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.ActivityRegistrationRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.SettingService
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import generated.Verzendingen
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BelcotaxService {

    @Autowired
    private lateinit var settingService: SettingService
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider
    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var dispatchService: DispatchService
    @Autowired
    private lateinit var formService: FormService
    @Autowired
    private lateinit var mailService: MailService

    fun getDispatchForFiscalYearAndRate(fiscalYear: Int): Verzendingen {
        val (beginOfYear, endOfYear) = getPeriod(fiscalYear)
        val activities = registrationRepository.getByStartBetweenOrderByStart(beginOfYear, endOfYear).filter(::relevantActivity)
        val forms = activities.groupBy { it.user }.flatMap { (user, activities) -> activities.asForms(user, fiscalYear) }
        return dispatchService.createDispatch(organizationProvider.getOwner(), organizationProvider.getCertifier(), forms)
    }

    fun getFormsForUserFiscalYearAndRate(username: String, fiscalYear: Int): List<ByteArray> {
        val (beginOfYear, endOfYear) = getPeriod(fiscalYear)
        val user = userDataProvider.getUser(username)
        val activities = registrationRepository.getByUserAndStartBetweenOrderByStart(user, beginOfYear, endOfYear).filter(::relevantActivity)
        check(activities.isNotEmpty()) { "No relevant activities found for $username" }
        val owner = organizationProvider.getOwner()
        val certifier = organizationProvider.getCertifier()
        return activities.asForms(user, fiscalYear).map { formService.createForm(owner, certifier, it) }
    }

    fun getFormsForFiscalYearAndRate(fiscalYear: Int): Map<User, List<ByteArray>> {
        val (beginOfYear, endOfYear) = getPeriod(fiscalYear)
        val activities = registrationRepository.getByStartBetweenOrderByStart(beginOfYear, endOfYear).filter(::relevantActivity)
        val owner = organizationProvider.getOwner()
        val certifier = organizationProvider.getCertifier()
        return activities.groupBy { it.user }.flatMap { (user, activities) -> activities.asForms(user, fiscalYear) }
            .groupBy(DeclarationFormDTO::user) { formService.createForm(owner, certifier, it) }
    }

    fun mailFormsToUser(fiscalYear: Int, user: User, forms: List<ByteArray>) {
        val params = mapOf("member.first.name" to user.firstName, "fiscal-year" to fiscalYear)
        val mailBuilder = mailService.builder()
            .to(user.email)
            .subject("Fiscaal attest kinderopvang $fiscalYear")
            .template("declaration-form-confirmation.html", params)
        forms.forEach { mailBuilder.addAttachment(it, "form.pdf", "application/pdf") }
        mailBuilder.send()
    }

    private fun getPeriod(fiscalYear: Int): Pair<LocalDateTime, LocalDateTime> {
        val beginOfYear = LocalDateTime.of(fiscalYear, 1, 1, 0, 0, 0, 0)
        val endOfYear = LocalDateTime.of(fiscalYear, 12, 31, 23, 59, 59, 999999999)
        return beginOfYear to endOfYear
    }

    private fun relevantActivity(registration: ActivityRegistration): Boolean {
        return registration.user.getAge(registration.start.toLocalDate()) < if (registration.user.hasHandicap) 21 else 14
    }

    private fun List<ActivityRegistration>.asForms(user: User, fiscalYear: Int): List<DeclarationFormDTO> {
        val rate = settingService.getRateForFiscalYear(fiscalYear)
        return chunked(4).mapIndexed { index, it ->
            DeclarationFormDTO(user, it[0], it.getOrNull(1), it.getOrNull(2), it.getOrNull(3), rate, index)
        }
    }
}