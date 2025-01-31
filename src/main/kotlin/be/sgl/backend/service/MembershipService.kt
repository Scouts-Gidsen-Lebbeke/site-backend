package be.sgl.backend.service

import be.sgl.backend.dto.UserRegistrationDTO
import be.sgl.backend.entity.PaymentStatus
import be.sgl.backend.repository.MembershipRepository
import be.sgl.backend.service.payment.CheckoutProvider
import be.sgl.backend.service.user.UserDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MembershipService {

    @Autowired
    private lateinit var membershipRepository: MembershipRepository
    @Autowired
    private lateinit var userDataProvider: UserDataProvider
    @Autowired
    private lateinit var checkoutProvider: CheckoutProvider

    fun createMembershipForNewUser(registrationDTO: UserRegistrationDTO): String {
        // Step 1: extra validations: age restriction, branch limitations, ...
        // Step 2: check if a user with this info already exists
        //   => If new user, payment was ongoing, return existing url
        //   => If existing user, throw error with info how to retrieve login
        // Step 3: create and persist the accompanying user (delegate to UserDataProvider)
        // Step 4: create a membership for the returned user
        // Step 5: create a payment for the membership
        return ""
    }

    fun updatePayment(paymentId: String) {
        val membership = membershipRepository.getMembershipByPaymentId(paymentId) ?: return
        when (checkoutProvider.getPaymentStatusById(paymentId)) {
            PaymentStatus.PAID -> {
                if (membership.paid) return
                if (membership.user.username != null) {
                    membership.markPaid()
                    membershipRepository.save(membership)
                    // TODO("send payment received confirmation email")
                } else {
                    userDataProvider.acceptRegistration(membership.user)
                }
            }
            PaymentStatus.CANCELLED -> {
                check(!membership.paid) { "This membership should never have been marked as paid!" }
                membershipRepository.delete(membership)
                if (membership.user.username == null) {
                    userDataProvider.denyRegistration(membership.user)
                }
            }
            PaymentStatus.REFUNDED -> {
                check(membership.paid) { "This membership should have been marked as paid!" }
                membershipRepository.delete(membership)
                // TODO("send payment refunded confirmation email")
            }
        }
    }
}