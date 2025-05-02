package be.sgl.backend.service.membership

import be.sgl.backend.alert.AlertCode
import be.sgl.backend.alert.AlertLogger
import be.sgl.backend.dto.Customer
import be.sgl.backend.dto.MembershipDTO
import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.membership.Membership
import be.sgl.backend.entity.user.User
import be.sgl.backend.mapper.MembershipMapper
import be.sgl.backend.repository.BranchRepository
import be.sgl.backend.repository.RoleRepository
import be.sgl.backend.repository.membership.MembershipPeriodRepository
import be.sgl.backend.repository.membership.MembershipRepository
import be.sgl.backend.service.PaymentService
import be.sgl.backend.service.exception.BranchNotFoundException
import be.sgl.backend.service.exception.MembershipNotFoundException
import be.sgl.backend.service.user.UserDataProvider
import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
    private lateinit var roleRepository: RoleRepository
    @Autowired
    private lateinit var mapper: MembershipMapper
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var alertLogger: AlertLogger
    @Autowired
    private lateinit var validateAndCreateMembership: ValidateAndCreateMembership
    @Autowired
    private lateinit var createCertificateForMembership: CreateCertificateForMembership

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
                throw IllegalStateException("This user already exists, contact the organization to retrieve the login!")
            }
            val membershipInProgress = paymentRepository.getCurrentPossiblyUnpaidByUser(it)
            if (membershipInProgress == null) {
                alertLogger.alert(AlertCode.NEW_USER_EXISTS_NO_MEMBERSHIP) {
                    "User ${it.id} isn't linked to any membership!"
                }
                throw IllegalStateException("This user already exists as an old unlinked account, " +
                        "contact the organization for help with creating the account!")
            }
            if (membershipInProgress.paid) {
                alertLogger.alert(AlertCode.NEW_USER_EXISTS_PAID_MEMBERSHIP) {
                    "User ${it.id} has active membership #${membershipInProgress.id} but no username!"
                }
                throw IllegalStateException("This user already exists but was not yet linked to an account, " +
                        "check the e-mail you received upon registration to create an account!")
            }
            logger.debug { "Previous registration exists but was unpaid, returning old checkout url" }
            return checkoutProvider.getCheckoutUrl(membershipInProgress)
        }
        val newUser = userDataProvider.registerUser(registrationDTO)
        return createMembershipForUser(newUser)
    }

    private fun createMembershipForUser(user: User): String {
        val currentPeriod = membershipPeriodRepository.getActivePeriod()
        val membership = paymentRepository.save(validateAndCreateMembership.execute(currentPeriod, user))
        val checkoutUrl = checkoutProvider.createCheckoutUrl(Customer(user), membership, "memberships", currentPeriod.id)
        paymentRepository.save(membership)
        return checkoutUrl
    }

    override fun handlePaymentPaid(payment: Membership) {
        roleRepository.getRoleToSyncByBranch(payment.branch)?.let {
            logger.info { "Membership ${payment.id} to branch ${payment.branch.name} requires role ${it.name}, assigning it..." }
            userDataProvider.startRole(payment.user, it)
        }
        val params = mapOf(
            "member" to payment.user.firstName,
            "price" to payment.price,
            "periodName" to payment.period.toString(),
            "branchName" to payment.branch.name
        )
        val mailBuilder = mailService.builder()
            .to(payment.user.email)
            .subject("Bevestiging inschrijving")
        if (payment.user.username == null) {
            logger.debug { "Membership for new user, also accepting linked registration..." }
            userDataProvider.acceptRegistration(payment.user)
            mailBuilder.template("membership-new-user-confirmation.html", params)
        } else {
            mailBuilder.template("membership-confirmation.html", params)
        }
        mailBuilder.send()
    }

    override fun handlePaymentCanceled(payment: Membership) {
        if (payment.user.username == null) {
            userDataProvider.denyRegistration(payment.user)
        }
    }

    override fun handlePaymentRefunded(payment: Membership) {
        //  membership payments aren't supported with a normal flow, only possible via payment provider
    }

    fun getCertificateForMembership(id: Int): ByteArray {
        val membership = getMembershipById(id)
        return createCertificateForMembership.execute(membership)
    }

    private fun getMembershipById(id: Int): Membership {
        return paymentRepository.findById(id).orElseThrow { MembershipNotFoundException() }
    }

    private fun getBranchById(id: Int): Branch {
        return branchRepository.findById(id).orElseThrow { BranchNotFoundException() }
    }
}