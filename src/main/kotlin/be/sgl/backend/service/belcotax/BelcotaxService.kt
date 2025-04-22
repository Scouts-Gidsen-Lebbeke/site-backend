package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormDTO
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.setting.SettingId.LATEST_DISPATCH_RATE
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.SettingService
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
    private lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var dispatchService: DispatchService
    @Autowired
    private lateinit var formService: FormService
    @Autowired
    private lateinit var mailService: MailService

    fun getDispatchForPreviousYear(): Verzendingen {
        val (beginOfYear, endOfYear) = getPreviousYearPeriod()
        val activities = registrationRepository.getPaidRegistrationsBetween(beginOfYear, endOfYear).filter(::relevantActivity)
        val forms = activities.groupBy { it.user }.flatMap { (user, activities) -> activities.asForms(user) }
        return dispatchService.createDispatch(forms)
    }

    fun getFormsForUserAndPreviousYear(username: String): List<ByteArray> {
        val (beginOfYear, endOfYear) = getPreviousYearPeriod()
        val user = userDataProvider.getUser(username)
        val activities = registrationRepository.getPaidRegistrationsForUserBetween(user, beginOfYear, endOfYear).filter(::relevantActivity)
        check(activities.isNotEmpty()) { "No relevant activities found for $username" }
        return activities.asForms(user).map(formService::createForm)
    }

    fun getFormsForPreviousYear(): Map<User, List<ByteArray>> {
        val (beginOfYear, endOfYear) = getPreviousYearPeriod()
        val activities = registrationRepository.getPaidRegistrationsBetween(beginOfYear, endOfYear).filter(::relevantActivity)
        return activities.groupBy { it.user }.flatMap { (user, activities) -> activities.asForms(user) }
            .groupBy(DeclarationFormDTO::user, formService::createForm)
    }

    fun mailFormsToUser(user: User, forms: List<ByteArray>) {
        val fiscalYear = LocalDateTime.now().year - 1
        val params = mapOf("member.first.name" to user.firstName, "fiscal-year" to fiscalYear)
        val mailBuilder = mailService.builder()
            .to(user.email)
            .subject("Fiscaal attest kinderopvang $fiscalYear")
            .template("declaration-form-confirmation.html", params)
        forms.forEach { mailBuilder.addAttachment(it, "form.pdf", "application/pdf") }
        mailBuilder.send()
    }

    private fun getPreviousYearPeriod(): Pair<LocalDateTime, LocalDateTime> {
        val fiscalYear = LocalDateTime.now().year - 1
        val beginOfYear = LocalDateTime.of(fiscalYear, 1, 1, 0, 0, 0, 0)
        val endOfYear = LocalDateTime.of(fiscalYear, 12, 31, 23, 59, 59, 999999999)
        return beginOfYear to endOfYear
    }

    private fun relevantActivity(registration: ActivityRegistration): Boolean {
        return registration.user.getAge(registration.start.toLocalDate()) < if (registration.user.hasHandicap) 21 else 14
    }

    private fun List<ActivityRegistration>.asForms(user: User): List<DeclarationFormDTO> {
        val rate = settingService.getOrDefault(LATEST_DISPATCH_RATE, 14.4)
        return chunked(4).mapIndexed { index, it ->
            DeclarationFormDTO(user, it[0], it.getOrNull(1), it.getOrNull(2), it.getOrNull(3), rate, index)
        }
    }
}