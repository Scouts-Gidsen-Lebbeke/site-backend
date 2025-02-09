package be.sgl.backend.service

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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.temporal.TemporalAdjusters.lastDayOfYear

@Service
class MembershipService {

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

    fun createMembershipForExistingUser(username: String): String {
        val user = userDataProvider.getUser(username)
        check(membershipRepository.getCurrentByUser(user) == null) { "This user already has an active membership!" }
        return createMembershipForUser(user)
    }

    fun createMembershipForNewUser(registrationDTO: UserRegistrationDTO): String {
        // Step 1: extra validations: age restriction, branch limitations, ...
        TODO("")
        // Step 2: check if a user with this info already exists
        //   => If new user, payment was ongoing, return existing url
        //   => If existing user, throw error with info how to retrieve login
        TODO("")
        // Step 3: create and persist the accompanying user (delegate to UserDataProvider)
        val newUser = userDataProvider.registerUser(registrationDTO)
        // Step 4: create a membership for the returned user
        return createMembershipForUser(newUser)
    }

    private fun createMembershipForUser(user: User): String {
        val currentPeriod = membershipPeriodRepository.getActivePeriod()
        val branch = determineCurrentBranchForUser(user, currentPeriod)
        check(branch != null) { "No active branch can be linked to a user of this age and sex!" }
        // TODO: branch limits
        val price = currentPeriod.restrictions.find { it.branch == branch }?.alternativePrice ?: currentPeriod.price
        val membership = Membership(user, currentPeriod, branch, price)
        val checkoutUrl = checkoutProvider.createCheckoutUrl(user, membership, "membership")
        membershipRepository.save(membership)
        return checkoutUrl
    }

    fun updatePayment(paymentId: String) {
        val membership = membershipRepository.getMembershipByPaymentId(paymentId) ?: return
        when (checkoutProvider.getPaymentStatusById(paymentId)) {
            SimplifiedPaymentStatus.PAID -> {
                if (membership.paid) return
                membership.markPaid()
                membershipRepository.save(membership)
                if (membership.user.username == null) {
                    userDataProvider.acceptRegistration(membership.user)
                }
                val params = mapOf(
                    "member.first.name" to membership.user.firstName,
                    "membership.price" to membership.price,
                    "membership.period.name" to membership.period,
                    "membership.branch.name" to membership.branch.name,
                )
                mailService.builder()
                    .to(membership.user.userData.email)
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
            else -> {}
        }
    }

    private fun determineCurrentBranchForUser(user: User, period: MembershipPeriod): Branch? {
        val age = user.userData.getAge(period.end.with(lastDayOfYear())) + user.userData.ageDeviation
        return branchRepository.getPossibleBranchesForSexAndAge(user.userData.sex, age).firstOrNull()
    }
}