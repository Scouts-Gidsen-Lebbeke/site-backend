package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.dto.ActivityRegistrationStatus
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.ActivityRegistrationRepository
import be.sgl.backend.repository.ActivityRepository
import be.sgl.backend.repository.ActivityRestrictionRepository
import be.sgl.backend.service.exception.ActivityNotFoundException
import be.sgl.backend.service.exception.RestrictionNotFoundException
import be.sgl.backend.mapper.ActivityMapper
import be.sgl.backend.repository.MembershipRepository
import be.sgl.backend.service.MailService
import be.sgl.backend.service.exception.RegistrationNotFoundException
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.base64Encoded
import be.sgl.backend.util.fillForm
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ActivityRegistrationService {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var activityRepository: ActivityRepository
    @Autowired
    private lateinit var registrationRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var restrictionRepository: ActivityRestrictionRepository
    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider
    @Autowired
    private lateinit var mapper: ActivityMapper
    @Autowired
    private lateinit var checkoutProvider: CheckoutProvider
    @Autowired
    private lateinit var mailService: MailService

    fun getAllRegistrationsForActivity(id: Int): List<ActivityRegistrationDTO> {
        val activity = getActivityById(id)
        return registrationRepository.getBySubscribable(activity).map(mapper::toDto)
    }

    fun getAllRegistrationsForUser(username: String): List<ActivityRegistrationDTO> {
        val user = userDataProvider.getUser(username)
        return registrationRepository.getByUser(user).map(mapper::toDto)
    }

    fun createPaymentForActivity(id: Int, restrictionId: Int, username: String, additionalData: String?): String {
        val user = userDataProvider.getUser(username)
        val activity = getActivityById(id)
        checkNotNull(registrationRepository.getByUserAndSubscribable(user, activity)) { "Registration for user ${user.username} already exists!" }
        checkNotNull(membershipRepository.getCurrentByUser(user)) { "User ${user.username} has no active membership!" }
        val restriction = getActivityRestrictionById(restrictionId)
        check(!isGlobalLimitReached(restriction.activity)) { "The limit for this activity is reached!" }
        check(!isRestrictionLimitReached(restriction)) { "The limit for this restriction is reached!" }
        check(!isBranchLimitReached(activity, restriction.branch)) { "The limit for this branch is reached!" }
        check(userDataProvider.getMedicalRecord(user)?.isUpToDate == true) { "User ${user.username} has no active medical record!" }
        val finalPrice = calculatePriceForActivity(user, activity, restriction, additionalData)
        var registration = ActivityRegistration(user, restriction, finalPrice, additionalData)
        registration = registrationRepository.save(registration)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(user, registration, "activity")
        registrationRepository.save(registration)
        return checkoutUrl
    }

    fun getStatusForActivityAndUser(activityId: Int, username: String): ActivityRegistrationStatus {
        val activity = getActivityById(activityId)
        val user = userDataProvider.getUser(username)
        registrationRepository.getByUserAndSubscribable(user, activity)?.let {
            return ActivityRegistrationStatus(mapper.toDto(it))
        }
        val membership = membershipRepository.getCurrentByUser(user) ?: return ActivityRegistrationStatus(activeMembership = false)
        val relevantRestrictions = activity.getRestrictionsForBranch(membership.branch)
        if (isGlobalLimitReached(activity) || isBranchLimitReached(activity, membership.branch)) {
            val closedOptions = relevantRestrictions.map(mapper::toDto)
            return ActivityRegistrationStatus(closedOptions = closedOptions)
        }
        val (closed, open) = restrictionRepository.findAllByBranch(membership.branch).partition(::isRestrictionLimitReached)
        val medicalRecord = userDataProvider.getMedicalRecord(user)
        return ActivityRegistrationStatus(
            openOptions = open.map(mapper::toDto),
            closedOptions = closed.map(mapper::toDto),
            medicsDate = medicalRecord?.lastModifiedDate,
            medicalsUpToDate = medicalRecord?.isUpToDate ?: false
        )
    }

    private fun isGlobalLimitReached(activity: Activity): Boolean {
        val globalLimit = activity.registrationLimit ?: return false
        return registrationRepository.getBySubscribable(activity).count() < globalLimit
    }

    private fun isRestrictionLimitReached(restriction: ActivityRestriction): Boolean {
        val restrictionLimit = restriction.alternativeLimit ?: return false
        return registrationRepository.getByRestriction(restriction).count() < restrictionLimit
    }

    private fun isBranchLimitReached(activity: Activity, branch: Branch): Boolean {
        val branchLimit = activity.restrictions.find { it.branch == branch && it.isBranchLimit() } ?: return false
        return registrationRepository.getByBranch(branch).count() < branchLimit.alternativeLimit!!
    }

    private fun calculatePriceForActivity(user: User, activity: Activity, restriction: ActivityRestriction, additionalData: String?): Double {
        var finalPrice = restriction.alternativePrice ?: activity.price
        val additionalPrice = activity.readAdditionalData(additionalData)
        if (user.hasReduction) {
            return finalPrice / activity.reductionFactor + additionalPrice
        }
        finalPrice += additionalPrice
        if (user.siblings.any { !it.hasReduction && registrationRepository.existsBySubscribableAndUser(activity, it) }) {
            return (finalPrice - activity.siblingReduction).coerceAtLeast(0.0)
        }
        return finalPrice
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }

    private fun getActivityRestrictionById(id: Int): ActivityRestriction {
        return restrictionRepository.findById(id).orElseThrow { RestrictionNotFoundException() }
    }

    fun updatePayment(paymentId: String) {
        logger.debug { "Update payment request for activity registration with paymentId $paymentId..." }
        val registration = registrationRepository.getByPaymentId(paymentId)
        if (registration == null) {
            logger.warn { "Registration with paymentId $paymentId not found!" }
            return
        }
        when (checkoutProvider.getPaymentStatusById(paymentId)) {
            SimplifiedPaymentStatus.PAID -> {
                if (registration.paid) {
                    logger.info { "Paid payment update received for registration already marked as paid, skipped." }
                    return
                }
                logger.debug { "Paid payment, marking registration as paid and notifying user..." }
                registration.markPaid()
                registrationRepository.save(registration)
                val params = mapOf(
                    "member.first.name" to registration.user.firstName,
                    "activity.price" to registration.price,
                    "activity.name" to registration.subscribable.name,
                )
                mailService.builder()
                    .to(registration.user.email)
                    .subject("Bevestiging inschrijving")
                    .template("activity-confirmation.html", params)
                    .send()
            }
            SimplifiedPaymentStatus.CANCELLED -> {
                check(!registration.paid) { "This registration should never have been marked as paid!" }
                registrationRepository.delete(registration)
            }
            SimplifiedPaymentStatus.REFUNDED -> {
                check(registration.paid) { "This registration should have been marked as paid!" }
                registrationRepository.delete(registration)
                // TODO("send payment refunded confirmation email")
            }
            else -> {
                // do nothing, the payment is still ongoing
            }
        }
        logger.debug { "Update registration payment request handled." }
    }

    fun getCertificateForRegistration(id: Int): ByteArray {
        val registration = registrationRepository.findById(id).orElseThrow { RegistrationNotFoundException() }
        val owner = organizationProvider.getOwner()
        val formData = mapOf(
            "name" to registration.user.firstName,
            "first_name" to registration.user.firstName,
            "birth_date" to registration.user.birthdate,
            "nis_nr" to registration.user.nis,
            "address" to registration.user.getHomeAddress(),
            "activity_name" to registration.subscribable.name,
            "period" to "${registration.start} - ${registration.end}",
            "days" to registration.calculateDays(),
            "amount" to "€ ${registration.price}",
            "payment_date" to registration.createdDate,
            "organization_name" to owner.name,
            "organization_address" to owner.address,
            "organization_email" to owner.getEmail(),
            "signature_date" to LocalDate.now(),
            "signatory" to "", // TODO
            "id" to "${registration.subscribable.id}-#${registration.id}".base64Encoded()
        )
        return fillForm("forms/participation.pdf", formData, "signature.png")
    }
}