package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormDTO
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.exception.LocalizedException
import be.sgl.backend.service.exception.UserNotFoundException
import generated.Verzendingen
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class BelcotaxService(
    private val findRelevantRegistrations: FindRelevantRegistrations,
    private val filterIntoValidFormData: FilterIntoValidFormData,
    private val userRepository: UserRepository,
    private val findRelevantUserRegistrations: FindRelevantUserRegistrations,
    private val dispatchService: DispatchService,
    private val formService: FormService,
    private val mailService: MailService
) {

    private val logger = KotlinLogging.logger {}

    fun getDispatchForPreviousYear(): Verzendingen {
        logger.info { "Creating dispatch for latest fiscal year..." }
        val forms = findRelevantRegistrations.forPreviousYear()
            .groupBy { it.user }
            .flatMap { (user, registrations) -> filterIntoValidFormData.execute(user, registrations) }
        if (forms.isEmpty()) {
            throw LocalizedException("belcotax.service.dispatch.no.activities")
        }
        logger.info { "Mapped registrations to dispatch data, ${forms.size} forms left." }
        return dispatchService.createDispatch(forms)
    }

    fun getFormsForUserAndPreviousYear(username: String): List<ByteArray> {
        val user = userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
        val registrations = findRelevantUserRegistrations.forPreviousYear(user)
        val forms = filterIntoValidFormData.execute(user, registrations)
        if (forms.isEmpty()) {
            throw LocalizedException("belcotax.service.forms.user.no.activities")
        }
        return forms.map(formService::createForm)
    }

    fun getFormsForPreviousYear(): Map<User, List<ByteArray>> {
        logger.info { "Creating forms for latest fiscal year..." }
        return findRelevantRegistrations.forPreviousYear()
            .groupBy { it.user }
            .flatMap { (user, registrations) -> filterIntoValidFormData.execute(user, registrations) }
            .groupBy(DeclarationFormDTO::user, formService::createForm)
    }

    fun mailFormsToUser(user: User, forms: List<ByteArray>) {
        val fiscalYear = LocalDateTime.now().year - 1
        val params = mapOf(
            "member" to user.firstName,
            "fiscalYear" to fiscalYear
        )
        val mailBuilder = mailService.builder()
            .to(user.email)
            .subject("Fiscaal attest kinderopvang $fiscalYear")
            .template("declaration-form-confirmation.html", params)
        forms.forEach { mailBuilder.addAttachment(it, "form.pdf", "application/pdf") }
        mailBuilder.send()
    }
}