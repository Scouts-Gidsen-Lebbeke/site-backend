package be.sgl.backend.service.membership

import be.sgl.backend.alert.AlertCode
import be.sgl.backend.alert.AlertLogger
import be.sgl.backend.dto.Customer
import be.sgl.backend.dto.MembershipDTO
import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.membership.MembershipPeriod
import be.sgl.backend.entity.user.User
import be.sgl.backend.mapper.MembershipMapper
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.service.PaymentService
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.service.exception.MembershipNotFoundException
import be.sgl.backend.service.organization.OrganizationProvider
import be.sgl.backend.service.user.UserDataProvider
import be.sgl.backend.util.base64Encoded
import be.sgl.backend.util.belgian
import be.sgl.backend.util.fillForm
import be.sgl.backend.util.pricePrecision
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfYear

@Service
@Transactional
class MembershipService : PaymentService<Membership, MembershipRepository>() {

    private val logger = KotlinLogging.logger {}

    @Autowired
    override lateinit var paymentRepository: MembershipRepository
    @Autowired
    private lateinit var membershipPeriodRepository: MembershipPeriodRepository
    @Autowired
    private lateinit var branchRepository: BranchRepository
    @Autowired
    private lateinit var mapper: MembershipMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var organizationProvider: OrganizationProvider
    @Autowired
    private lateinit var alertLogger: AlertLogger
    @Value("\${spring.application.base-url}")
    private lateinit var baseUrl: String

    fun getAllMembershipsForUser(username: String): List<MembershipDTO> {
        val user = userDataProvider.getUser(username)
        return paymentRepository.getMembershipsByUser(user).map(mapper::toDto)
    }

    fun getCurrentMembershipForUser(username: String): MembershipDTO? {
        val user = userDataProvider.getUser(username)
        return paymentRepository.getCurrentByUser(user)?.run(mapper::toDto)
    }

    fun getCurrentMembershipsForBranch(branchId: Int?): List<MembershipDTO> {
        if (branchId == null) {
            return paymentRepository.getCurrent().map(mapper::toDto)
        }
        val branch = getBranchById(branchId)
        return paymentRepository.getCurrentByBranch(branch).map(mapper::toDto)
    }

    fun getMembershipDTOById(id: Int): MembershipDTO? {
        return paymentRepository.findById(id).map(mapper::toDto).orElse(null)
    }

    fun createMembershipForExistingUser(username: String): String {
        logger.debug { "New membership creation request for user $username..." }
        val user = userDataProvider.getUser(username)
        paymentRepository.getCurrentByUser(user)?.let {
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
            val membershipInProgress = paymentRepository.getCurrentByUser(it)
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
            val periodCount = paymentRepository.countByPeriod(currentPeriod)
            check(periodCount < it) { "This period already has its maximum number of members!" }
        }
        val branch = determineCurrentBranchForUser(user, currentPeriod)
        check(branch != null) { "No active branch can be linked to a user of this age and sex!" }
        val branchRestriction = currentPeriod.restrictions.find { it.branch == branch }
        branchRestriction?.registrationLimit?.let {
            val branchCount = paymentRepository.countByPeriodAndBranch(currentPeriod, branch)
            check(branchCount < it) { "This branch already has its maximum number of members!" }
        }
        val price = branchRestriction?.alternativePrice ?: currentPeriod.price
        val membership = paymentRepository.save(Membership(user, currentPeriod, branch, price))
        val checkoutUrl = checkoutProvider.createCheckoutUrl(Customer(user), membership, "memberships", currentPeriod.id)
        paymentRepository.save(membership)
        return checkoutUrl
    }

    private fun determineCurrentBranchForUser(user: User, period: MembershipPeriod): Branch? {
        val age = user.getAge(period.end.with(lastDayOfYear())) + user.ageDeviation
        return branchRepository.getPossibleBranchesForSexAndAge(user.sex, age).firstOrNull()
    }

    override fun handlePaymentPaid(payment: Membership) {
        if (payment.user.username == null) {
            logger.debug { "Membership for new user, also accepting linked registration..." }
            userDataProvider.acceptRegistration(payment.user)
        }
        val params = mapOf(
            "member" to payment.user.firstName,
            "price" to payment.price,
            "periodName" to payment.period.toString(),
            "branchName" to payment.branch.name,
            "baseUrl" to baseUrl
        )
        mailService.builder()
            .to(payment.user.email)
            .subject("Bevestiging inschrijving")
            .template("subscription-confirmation.html", params)
            .send()
    }

    override fun handlePaymentCanceled(payment: Membership) {
        if (payment.user.username == null) {
            userDataProvider.denyRegistration(payment.user)
        }
    }

    override fun handlePaymentRefunded(payment: Membership) {
        TODO("send payment refunded confirmation email")
    }

    fun getCertificateForMembership(id: Int): ByteArray {
        val membership = getMembershipById(id)
        val user = userDataProvider.getUser(membership.user.username!!)
        val owner = organizationProvider.getOwner()
        val representative = organizationProvider.getRepresentative()
        val formData = mapOf(
            "name" to user.name,
            "first_name" to user.firstName,
            "birth_date" to user.birthdate.belgian(),
            "nis_nr" to user.nis,
            "address" to user.getHomeAddress(),
            "membership_period" to membership.period,
            "amount" to "€ ${membership.price.pricePrecision()}",
            "payment_date" to membership.createdDate?.belgian(),
            "organization_name" to owner.name,
            "organization_address" to owner.address,
            "organization_email" to owner.getEmail(),
            "signature_date" to LocalDate.now().belgian(),
            "signatory" to representative.user.getFullName(),
            "id" to "${membership.period.id}-#${membership.id}".base64Encoded()
        )
        return fillForm("forms/membership.pdf", formData, representative.signature)
    }

    private fun getMembershipById(id: Int): Membership {
        return paymentRepository.findById(id).orElseThrow { MembershipNotFoundException() }
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}