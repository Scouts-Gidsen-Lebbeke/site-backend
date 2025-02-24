package be.sgl.backend.service

import be.sgl.backend.alert.AlertCode
import be.sgl.backend.alert.AlertLogger
import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.SimplifiedPaymentStatus
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.user.User
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.MembershipPeriodRepository
import be.sgl.backend.repository.MembershipRepository
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.service.user.UserDataProvider
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.temporal.TemporalAdjusters.lastDayOfYear

@Service
@Transactional
class MembershipService {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var membershipPeriodRepository: MembershipPeriodRepository
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var checkoutProvider: CheckoutProvider
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var mailService: MailService
    @Autowired
    private lateinit var alertLogger: AlertLogger

    fun createMembershipForExistingUser(username: String): String {
        logger.debug { "New membership creation request for user $username..." }
        val user = userDataProvider.getUser(username)
        membershipRepository.getCurrentByUser(user)?.let {
            logger.error { "Active membership found for $username on creation request" }
            throw IllegalStateException("There is already an active membership for $username!")
        }
        return createMembershipForUser(user)
    }

    fun createMembershipForNewUser(registrationDTO: UserRegistrationDTO): String {
        logger.debug { "New membership creation request for new user..." }
        userDataProvider.findByNameAndEmail(registrationDTO.name, registrationDTO.firstName, registrationDTO.email)?.let {
            it.username?.let {
                logger.error { "User creation request for already existing user $it" }
                throw IllegalStateException("This user already exist, contact the organization to retrieve the login!")
            }
            val membershipInProgress = membershipRepository.getCurrentByUser(it)
            if (membershipInProgress == null) {
                alertLogger.alert(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP) {
                    "User ${it.id} isn't linked to any membership!"
                }
                throw RuntimeException()
            }
            // TODO: is paymentstatus ok
            logger.debug { "Previous registration exists but was unpaid, returning old checkout url" }
            return checkoutProvider.getCheckoutUrl(membershipInProgress)
        }
        val newUser = userDataProvider.registerUser(registrationDTO)
        return createMembershipForUser(newUser)
    }

    private fun createMembershipForUser(user: User): String {
        val currentPeriod = membershipPeriodRepository.getActivePeriod()
        currentPeriod.registrationLimit?.let {
            val periodCount = membershipRepository.countByPeriod(currentPeriod)
            check(periodCount < it) { "This period already has its maximum number of members!" }
        }
        val branch = determineCurrentBranchForUser(user, currentPeriod)
        check(branch != null) { "No active branch can be linked to a user of this age and sex!" }
        val branchRestriction = currentPeriod.restrictions.find { it.branch == branch }
        branchRestriction?.registrationLimit?.let {
            val branchCount = membershipRepository.countByPeriodAndBranch(currentPeriod, branch)
            check(branchCount < it) { "This branch already has its maximum number of members!" }
        }
        val price = branchRestriction?.alternativePrice ?: currentPeriod.price
        val membership = Membership(user, currentPeriod, branch, price)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(user, membership, "membership")
        membershipRepository.save(membership)
        return checkoutUrl
    }

    fun updatePayment(paymentId: String) {
        logger.debug { "Update payment request for membership with paymentId $paymentId..." }
        val membership = membershipRepository.getMembershipByPaymentId(paymentId)
        if (membership == null) {
            logger.warn { "Membership with paymentId $paymentId not found!" }
            return
        }
        when (checkoutProvider.getPaymentStatusById(paymentId)) {
            SimplifiedPaymentStatus.PAID -> {
                if (membership.paid) {
                    logger.info { "Paid payment update received for membership already marked as paid, skipped." }
                    return
                }
                logger.debug { "Paid payment, marking membership as paid and notifying member..." }
                membership.markPaid()
                membershipRepository.save(membership)
                if (membership.user.username == null) {
                    logger.debug { "Membership for new user, also accepting linked registration..." }
                    userDataProvider.acceptRegistration(membership.user)
                }
                val params = mapOf(
                    "member.first.name" to membership.user.firstName,
                    "membership.price" to membership.price,
                    "membership.period.name" to membership.period,
                    "membership.branch.name" to membership.branch.name,
                )
                mailService.builder()
                    .to(membership.user.email)
                    .subject("Bevestiging inschrijving")
                    .template("subscription-confirmation.html", params)
                    .send()
            }
            SimplifiedPaymentStatus.CANCELLED -> {
                check(!membership.paid) { "This membership should never have been marked as paid!" }
                membershipRepository.delete(membership)
                if (membership.user.username == null) {
                    userDataProvider.denyRegistration(membership.user)
                }
            }
            SimplifiedPaymentStatus.REFUNDED -> {
                check(membership.paid) { "This membership should have been marked as paid!" }
                membershipRepository.delete(membership)
                // TODO("send payment refunded confirmation email")
            }
            else -> {
                // do nothing, the payment is still ongoing
            }
        }
        logger.debug { "Update membership payment request handled." }
    }

    private fun determineCurrentBranchForUser(user: User, period: MembershipPeriod): Branch? {
        val age = user.getAge(period.end.with(lastDayOfYear())) + user.ageDeviation
        return branchRepository.getPossibleBranchesForSexAndAge(user.sex, age).firstOrNull()
    }
}