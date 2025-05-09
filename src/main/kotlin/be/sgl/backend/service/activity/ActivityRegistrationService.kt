package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.dto.ActivityRegistrationStatus
import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.registrable.RegistrableStatus
import be.sgl.backend.entity.registrable.RegistrableStatus.Companion.getStatus
import be.sgl.backend.entity.registrable.activity.Activity
import be.sgl.backend.entity.registrable.activity.ActivityRegistration
import be.sgl.backend.entity.registrable.activity.ActivityRestriction
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.activity.ActivityRegistrationRepository
import be.sgl.backend.repository.activity.ActivityRepository
import be.sgl.backend.repository.activity.ActivityRestrictionRepository
import be.sgl.backend.service.exception.ActivityNotFoundException
import be.sgl.backend.service.exception.RestrictionNotFoundException
import be.sgl.backend.mapper.ActivityMapper
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.repository.user.SiblingRepository
import be.sgl.backend.service.PaymentService
import be.sgl.backend.service.exception.ActivityRegistrationNotFoundException
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ActivityRegistrationService : PaymentService<ActivityRegistration, ActivityRegistrationRepository>() {

    private val logger = KotlinLogging.logger {}

    @Autowired
    override lateinit var paymentRepository: ActivityRegistrationRepository
    @Autowired
    private lateinit var activityRepository: ActivityRepository
    @Autowired
    private lateinit var restrictionRepository: ActivityRestrictionRepository
    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var siblingRepository: SiblingRepository
    @Autowired
    private lateinit var mapper: ActivityMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider

    fun getAllRegistrationsForActivity(id: Int): List<ActivityRegistrationDTO> {
        logger.info { "Fetching all registrations for activity #$id" }
        val activity = getActivityById(id)
        return paymentRepository.getPaidRegistrationsByActivity(activity).map(mapper::toDto)
    }

    fun getAllRegistrationsForUser(username: String): List<ActivityRegistrationDTO> {
        logger.info { "Fetching all registrations for user $username" }
        val user = userDataProvider.getUser(username)
        return paymentRepository.getByUser(user).map(mapper::toDto)
    }

    fun getActivityRegistrationDTOById(id: Int) : ActivityRegistrationDTO? {
        logger.info { "Fetching registration #$id" }
        return paymentRepository.findById(id).map(mapper::toDto).orElse(null)
    }

    fun getStatusForActivityAndUser(activityId: Int, username: String): ActivityRegistrationStatus {
        logger.info { "Checking registration status for activity #$activityId and user $username" }
        val activity = getActivityById(activityId)
        val user = userDataProvider.getUser(username)
        paymentRepository.getByUserAndSubscribable(user, activity)?.let {
            logger.info { "User already registered (#${it.id})" }
            return ActivityRegistrationStatus(mapper.toDto(it))
        }
        val relevantBranches = getCurrentValidBranchesForUser(user)
        if (relevantBranches.isEmpty()) {
            logger.info { "No active branch found for $username" }
            return ActivityRegistrationStatus(activeMembership = false)
        }
        val relevantRestrictions = activity.getRestrictionsForBranches(relevantBranches)
        if (relevantRestrictions.isEmpty()) {
            logger.info { "No applicable restrictions found for $username" }
            return ActivityRegistrationStatus()
        }
        if (isGlobalLimitReached(activity)) {
            logger.info { "Global activity limit (${activity.registrationLimit}) is reached for ${activity.name}" }
            return ActivityRegistrationStatus(closedOptions = relevantRestrictions.map(mapper::toDto))
        }
        val restrictionsWithoutBranchLimit = relevantRestrictions.filter { !isBranchLimitReached(activity, it.branch) }
        if (restrictionsWithoutBranchLimit.isEmpty()) {
            logger.info { "Branch limit is reached for all active branches of $username" }
            return ActivityRegistrationStatus(closedOptions = relevantRestrictions.map(mapper::toDto))
        }
        val (closed, open) = restrictionsWithoutBranchLimit.partition(::isRestrictionLimitReached)
        if (open.isEmpty()) {
            logger.info { "Limit is reached for each applicable restriction for $username" }
            return ActivityRegistrationStatus(closedOptions = closed.map(mapper::toDto))
        }
        if (user.hasReduction) {
            logger.info { "User is eligible for reduced tariff, altering open option prices..." }
            open.onEach { it.alternativePrice = (it.alternativePrice ?: activity.price).reducePrice(activity.reductionFactor) }
        }
        logger.info { "User has ${open.size} open activity options" }
        val medicalRecord = userDataProvider.getMedicalRecord(user)
        if (medicalRecord == null) {
            logger.info { "Medical record not found for $username" }
        } else if (!medicalRecord.isUpToDate) {
            logger.info { "Medical record for $username is older than one year" }
        }
        return ActivityRegistrationStatus(
            openOptions = open.map(mapper::toDto),
            closedOptions = closed.map(mapper::toDto),
            medicsDate = medicalRecord?.lastModifiedDate,
            medicalsUpToDate = medicalRecord?.isUpToDate ?: false
        )
    }

    private fun getCurrentValidBranchesForUser(user: User): List<Branch> {
        logger.info { "Retrieving current valid branch(es) for ${user.username}" }
        val activeBranch = membershipRepository.getCurrentByUser(user)?.let {
            logger.info { "Found active membership for branch ${it.branch.name} (#${it.id})" }
            it.branch
        }
        val branches = listOfNotNull(activeBranch).toMutableList()
        branchRepository.getPassiveBranches().filter { it.matchesUser(user) }.forEach {
            logger.info { "Found matching passive branch ${it.name}" }
            branches += it
        }
        return branches
    }

    fun createPaymentForActivity(id: Int, restrictionId: Int, username: String, additionalData: String?): String {
        logger.info { "Creating a payment for activity #$id (restriction #$restrictionId) and $username" }
        val user = userDataProvider.getUser(username)
        val activity = getActivityById(id)
        val status = getStatusForActivityAndUser(id, username)
        check(status.currentRegistration == null) { "Registration for user ${user.username} already exists!" }
        check(status.activeMembership) { "User ${user.username} has no active membership!" }
        val restriction = getActivityRestrictionById(restrictionId)
        check(status.openOptions.any { it.id == restrictionId }) { "The chosen restriction is not valid (anymore) for ${activity.name} and ${user.username}!" }
        // not yet active
        // check(status.medicalsUpToDate) { "User ${user.username} has no active medical record!" }
        val finalPrice = calculatePriceForActivity(user, activity, restriction, additionalData)
        logger.info { "Calculated price for this registration is €$finalPrice" }
        var registration = ActivityRegistration(activity, user, restriction, finalPrice, additionalData)
        registration = paymentRepository.save(registration)
        logger.info { "Created registration #${registration.id}" }
        if (finalPrice == 0.0) {
            logger.info { "Registration was free, returning redirect url immediately" }
            return checkoutProvider.createRedirectUrl(registration, "activities", activity.id)
        }
        logger.info { "Registration is not free, linking payment via payment provider" }
        val checkoutUrl = checkoutProvider.createCheckoutUrl(Customer(user), registration, "activities", activity.id)
        logger.info { "Registration linked to payment ${registration.paymentId}, saving reference" }
        paymentRepository.save(registration)
        logger.info { "Redirecting user to payment url $checkoutUrl" }
        return checkoutUrl
    }

    private fun calculatePriceForActivity(user: User, activity: Activity, restriction: ActivityRestriction, additionalData: String?): Double {
        logger.info { "Calculating price applicable to ${user.username} for activity #${activity.id} (restriction #${restriction.id})" }
        var finalPrice = restriction.alternativePrice ?: activity.price
        logger.info { "Calculated base price is €$finalPrice" }
        val additionalPrice = activity.readAdditionalData(additionalData)
        logger.info { "Additional cost from extra data is €$additionalPrice" }
        if (user.hasReduction) {
            logger.info { "User is eligible for reduced tariff, dividing base price with reduction factor (${activity.reductionFactor})" }
            return finalPrice.reducePrice(activity.reductionFactor) + additionalPrice
        }
        finalPrice += additionalPrice
        siblingRepository.getByUser(user).firstOrNull { !it.sibling.hasReduction && paymentRepository.existsBySubscribableAndUser(activity, it.sibling) }?.let {
            logger.info { "${user.username} has already subscribed sibling ${it.sibling.username}, applying sibling reduction" }
            return (finalPrice - activity.siblingReduction).coerceAtLeast(0.0)
        }
        return finalPrice
    }

    private fun isGlobalLimitReached(activity: Activity): Boolean {
        val globalLimit = activity.registrationLimit ?: return false
        return paymentRepository.countPaidRegistrationsByActivity(activity) >= globalLimit
    }

    private fun isRestrictionLimitReached(restriction: ActivityRestriction): Boolean {
        val restrictionLimit = restriction.alternativeLimit ?: return false
        return paymentRepository.countByRestriction(restriction) >= restrictionLimit
    }

    private fun isBranchLimitReached(activity: Activity, branch: Branch): Boolean {
        val branchLimit = activity.getBranchLimit(branch) ?: return false
        return paymentRepository.countByActivityAndBranch(activity, branch) >= branchLimit
    }

    override fun handlePaymentPaid(payment: ActivityRegistration) {
        if (!payment.subscribable.sendConfirmation) return
        val params = mapOf(
            "member" to payment.user.firstName,
            "price" to payment.price,
            "activityName" to payment.subscribable.name,
            "branchName" to payment.restriction.branch.name,
            "restrictionName" to payment.restriction.name,
            "additionalData" to payment.getAdditionalDataMap()
        )
        val mailBuilder = mailService.builder()
            .to(payment.user.email)
            .subject("Bevestiging inschrijving")
            .template("activity-confirmation.html", params)
        payment.subscribable.communicationCC?.let { mailBuilder.cc(it) }
        mailBuilder.send()
    }

    override fun handlePaymentRefunded(payment: ActivityRegistration) {
        val params = mapOf(
            "member" to payment.user.firstName,
            "price" to payment.price - 1,
            "activityName" to payment.subscribable.name,
        )
        val mailBuilder = mailService.builder()
            .to(payment.user.email)
            .subject("Annulatie inschrijving")
            .template("cancel-activity-confirmation.html", params)
        payment.subscribable.communicationCC?.let { mailBuilder.cc(it) }
        mailBuilder.send()
    }

    fun markRegistrationAsCompleted(id: Int) {
        logger.info { "Marking registration #$id as completed..." }
        val registration = getRegistrationById(id)
        check(registration.paid) { "Only a paid activity can be marked as completed!" }
        if (registration.completed) {
            logger.warn { "Registration is already marked as completed!" }
            return
        }
        check(registration.start.minusHours(1).isBefore(LocalDateTime.now())) { "Registrations can only be completed starting one hour before the activity!" }
        registration.completed = true
        paymentRepository.save(registration)
        if (registration.subscribable.sendCompleteConfirmation) {
            logger.info { "Linked activity requires completion confirmation, sending mail..." }
            val params = mapOf(
                "member" to registration.user.firstName,
                "activityName" to registration.subscribable.name
            )
            val mailBuilder = mailService.builder()
                .to(registration.user.email)
                .subject("Afwerking inschrijving")
                .template("activity-completion.html", params)
            registration.subscribable.communicationCC?.let { mailBuilder.cc(it) }
            mailBuilder.send()
        }
        logger.info { "Registration #$id successfully marked as completed" }
    }

    fun cancelRegistration(id: Int) {
        logger.info { "Cancelling activity registration #$id..." }
        val registration = getRegistrationById(id)
        check(registration.paid) { "Only a paid activity registration can be cancelled!" }
        check(registration.subscribable.getStatus() == RegistrableStatus.REGISTRATIONS_OPENED) { "Cancellation is only possible when registrations are still open!" }
        checkoutProvider.refundPayment(registration)
        logger.info { "Activity registration #$id successfully cancelled" }
    }

    fun getCertificateForRegistration(id: Int): ByteArray {
        val registration = getRegistrationById(id)
        check(registration.completed) { "A certificate can only be generated for a completed activity!" }
        val user = userDataProvider.getUser(registration.user.username!!)
        val owner = organizationProvider.getOwner()
        val representative = organizationProvider.getRepresentative()
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
        return fillForm("forms/participation.pdf", formData, representative.signature)
    }

    fun getPaymentForRegistration(id: Int): String {
        val registration = getRegistrationById(id)
        check(!registration.paid) { "This activity is already paid!" }
        return checkoutProvider.getCheckoutUrl(registration)
    }

    private fun getRegistrationById(id: Int): ActivityRegistration {
        return paymentRepository.findById(id).orElseThrow { ActivityRegistrationNotFoundException() }
    }

    private fun getActivityById(id: Int): Activity {
        return activityRepository.findById(id).orElseThrow { ActivityNotFoundException() }
    }

    private fun getActivityRestrictionById(id: Int): ActivityRestriction {
        return restrictionRepository.findById(id).orElseThrow { RestrictionNotFoundException() }
    }
}