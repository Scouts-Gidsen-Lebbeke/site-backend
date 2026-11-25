package be.sgl.backend.service.registrable.activity

import be.sgl.backend.entity.PaymentTestMother.membership
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.shared.Sex
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.repository.membership.MembershipRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class GetCurrentValidBranchesForUserTest {

    @Mock
    lateinit var membershipRepository: MembershipRepository
    @Mock
    lateinit var branchRepository: BranchRepository
    @InjectMocks
    lateinit var testee: GetCurrentValidBranchesForUser

    private val user = user().sex(Sex.MALE).birthdate(LocalDate.now().minusYears(30)).build()

    @Test
    fun `should return the branch of the active membership and matching passive branches`() {
        // given
        val branch = branch().id(1).build()
        val passiveBranch = branch().id(2).passive().sex(Sex.MALE).minimumAge(18).build()
        given(membershipRepository.getCurrentByUser(user)).willReturn(membership().user(user).branch(branch).build())
        given(branchRepository.getPassiveBranches()).willReturn(listOf(passiveBranch))

        // when
        val branches = testee.execute(user)

        // then
        assertThat(branches).containsExactly(branch, passiveBranch)
    }

    @Test
    fun `should return no branches when the user has no membership and no passive branch matches`() {
        // given
        val tooOldBranch = branch().id(2).passive().sex(Sex.MALE).minimumAge(40).build()
        given(membershipRepository.getCurrentByUser(user)).willReturn(null)
        given(branchRepository.getPassiveBranches()).willReturn(listOf(tooOldBranch))

        // when
        val branches = testee.execute(user)

        // then
        assertThat(branches).isEmpty()
    }

    @Test
    fun `should return only matching passive branches when the user has no membership`() {
        // given
        val passiveBranch = branch().id(2).passive().sex(Sex.MALE).minimumAge(18).build()
        val otherSexBranch = branch().id(3).passive().sex(Sex.FEMALE).build()
        given(membershipRepository.getCurrentByUser(user)).willReturn(null)
        given(branchRepository.getPassiveBranches()).willReturn(listOf(passiveBranch, otherSexBranch))

        // when
        val branches = testee.execute(user)

        // then
        assertThat(branches).containsExactly(passiveBranch)
    }

    @Test
    fun `should add the age deviation to the age of the user when matching passive branches`() {
        // given
        val minimumAge = 8
        val deviatingUser = user().sex(Sex.MALE).birthdate(LocalDate.now().minusYears(minimumAge - 1L)).ageDeviation(1).build()
        val passiveBranch = branch().id(2).passive().sex(Sex.MALE).minimumAge(minimumAge).build()
        given(membershipRepository.getCurrentByUser(deviatingUser)).willReturn(null)
        given(branchRepository.getPassiveBranches()).willReturn(listOf(passiveBranch))

        // when
        val branches = testee.execute(deviatingUser)

        // then
        assertThat(branches).containsExactly(passiveBranch)
    }
}
