package be.sgl.backend.service.registrable.activity

import be.sgl.backend.organization.domain.Branch
import be.sgl.backend.entity.user.User
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class GetCurrentValidBranchesForUser(
    private val membershipRepository: MembershipRepository,
    private val branchRepository: BranchRepository
) {

    private val logger = KotlinLogging.logger {}

    fun execute(user: User): List<Branch> {
        logger.info { "Retrieving current valid branch(es) for ${user.username}" }
        val activeBranch = membershipRepository.getCurrentByUser(user)?.let {
            logger.info { "Found active membership for branch ${it.branch.name} (#${it.id})" }
            it.branch
        }
        val branches = listOfNotNull(activeBranch).toMutableList()
        branchRepository.getPassiveBranches().filter { it.matches(user.getAge() + user.ageDeviation, user.sex) }.forEach {
            logger.info { "Found matching passive branch ${it.name}" }
            branches += it
        }
        return branches
    }
}