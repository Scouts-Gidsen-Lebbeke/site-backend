package be.sgl.backend.service.activity

import be.sgl.backend.dto.ActivityRegistrationDTO
import be.sgl.backend.dto.ActivityRegistrationStatus
import be.sgl.backend.dto.Customer
import be.sgl.backend.entity.branch.Branch
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
import be.sgl.backend.service.PaymentService
import be.sgl.backend.service.exception.ActivityRegistrationNotFoundException
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.base64Encoded
import be.sgl.backend.util.fillForm
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

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
    private lateinit var mapper: ActivityMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider

    fun getAllRegistrationsForActivity(id: Int): List<ActivityRegistrationDTO> {
        val activity = getActivityById(id)
        return paymentRepository.getPaidRegistrationsByActivity(activity).map(mapper::toDto)
    }

    fun getAllRegistrationsForUser(username: String): List<ActivityRegistrationDTO> {
        val user = userDataProvider.getUser(username)
        return paymentRepository.getByUser(user).map(mapper::toDto)
    }

    fun getActivityRegistrationDTOById(id: Int) : ActivityRegistrationDTO? {
        return paymentRepository.findById(id).map(mapper::toDto).orElse(null)
    }

    fun getStatusForActivityAndUser(activityId: Int, username: String): ActivityRegistrationStatus {
        val activity = getActivityById(activityId)
        val user = userDataProvider.getUser(username)
        paymentRepository.getByUserAndSubscribable(user, activity)?.let {
            return ActivityRegistrationStatus(mapper.toDto(it))
        }
        val relevantBranch = getRelevantBranchForUser(user) ?: return ActivityRegistrationStatus(activeMembership = false)
        val relevantRestrictions = activity.getRestrictionsForBranch(relevantBranch)
        if (isGlobalLimitReached(activity) || isBranchLimitReached(activity, relevantBranch)) {
            val closedOptions = relevantRestrictions.map(mapper::toDto)
            return ActivityRegistrationStatus(closedOptions = closedOptions)
        }
        val (closed, open) = relevantRestrictions.partition(::isRestrictionLimitReached)
        val medicalRecord = userDataProvider.getMedicalRecord(user)
        return ActivityRegistrationStatus(
            openOptions = open.map(mapper::toDto),
            closedOptions = closed.map(mapper::toDto),
            medicsDate = medicalRecord?.lastModifiedDate,
            medicalsUpToDate = medicalRecord?.isUpToDate ?: false
        )
    }

    private fun getRelevantBranchForUser(user: User): Branch? {
        val membership = membershipRepository.getCurrentByUser(user)
        val passiveBranch = branchRepository.getPassiveBranches().firstOrNull { it.matchesUser(user) }
        return membership?.branch ?: passiveBranch
    }

    fun createPaymentForActivity(id: Int, restrictionId: Int, username: String, additionalData: String?): String {
        val user = userDataProvider.getUser(username)
        val activity = getActivityById(id)
        checkNotNull(paymentRepository.getByUserAndSubscribable(user, activity)) { "Registration for user ${user.username} already exists!" }
        checkNotNull(getRelevantBranchForUser(user)) { "User ${user.username} has no active membership!" }
        val restriction = getActivityRestrictionById(restrictionId)
        check(!isGlobalLimitReached(restriction.activity)) { "The limit for this activity is reached!" }
        check(!isRestrictionLimitReached(restriction)) { "The limit for this restriction is reached!" }
        check(!isBranchLimitReached(activity, restriction.branch)) { "The limit for this branch is reached!" }
        // not yet active
        // check(userDataProvider.getMedicalRecord(user)?.isUpToDate == true) { "User ${user.username} has no active medical record!" }
        val finalPrice = calculatePriceForActivity(user, activity, restriction, additionalData)
        var registration = ActivityRegistration(user, restriction, finalPrice, additionalData)
        registration = paymentRepository.save(registration)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(Customer(user), registration, "activities", activity.id)
        paymentRepository.save(registration)
        return checkoutUrl
    }

    private fun calculatePriceForActivity(user: User, activity: Activity, restriction: ActivityRestriction, additionalData: String?): Double {
        var finalPrice = restriction.alternativePrice ?: activity.price
        val additionalPrice = activity.readAdditionalData(additionalData)
        if (user.hasReduction) {
            return finalPrice / activity.reductionFactor + additionalPrice
        }
        finalPrice += additionalPrice
        if (user.siblings.any { !it.hasReduction && paymentRepository.existsBySubscribableAndUser(activity, it) }) {
            return (finalPrice - activity.siblingReduction).coerceAtLeast(0.0)
        }
        return finalPrice
    }

    private fun isGlobalLimitReached(activity: Activity): Boolean {
        val globalLimit = activity.registrationLimit ?: return false
        return paymentRepository.getPaidRegistrationsByActivity(activity).count() < globalLimit
    }

    private fun isRestrictionLimitReached(restriction: ActivityRestriction): Boolean {
        val restrictionLimit = restriction.alternativeLimit ?: return false
        return paymentRepository.getByRestriction(restriction).count() < restrictionLimit
    }

    private fun isBranchLimitReached(activity: Activity, branch: Branch): Boolean {
        val branchLimit = activity.restrictions.find { it.branch == branch && it.isBranchLimit() } ?: return false
        return paymentRepository.getByActivityAndBranch(activity, branch).count() < branchLimit.alternativeLimit!!
    }

    override fun handlePaymentPaid(payment: ActivityRegistration) {
        val params = mapOf(
            "member.first.name" to payment.user.firstName,
            "activity.price" to payment.price,
            "activity.name" to payment.subscribable.name,
        )
        mailService.builder()
            .to(payment.user.email)
            .subject("Bevestiging inschrijving")
            .template("activity-confirmation.html", params)
            .send()
    }

    override fun handlePaymentRefunded(payment: ActivityRegistration) {
        // TODO("send payment refunded confirmation email")
    }

    fun getCertificateForRegistration(id: Int): ByteArray {
        val registration = getRegistrationById(id)
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