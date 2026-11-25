package be.sgl.backend.organization.application

import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import be.sgl.backend.organization.BranchSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class BranchApiImplTest {

    @Mock
    lateinit var branchRepository: BranchRepository
    @InjectMocks
    lateinit var testee: BranchApiImpl

    private val id = 1

    @Test
    fun `should report active when branch status is ACTIVE`() {
        // given
        given(branchRepository.findById(id)).willReturn(Optional.of(branch().id(id).active().build()))

        // when
        val result = testee.isActive(id)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `should report inactive when branch status is not ACTIVE`() {
        // given
        given(branchRepository.findById(id)).willReturn(Optional.of(branch().id(id).passive().build()))

        // when
        val result = testee.isActive(id)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `should report inactive when branch is unknown`() {
        // given
        given(branchRepository.findById(id)).willReturn(Optional.empty())

        // when
        val result = testee.isActive(id)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `should map id name and image when finding summaries`() {
        // given
        val first = branch().id(1).name("Kapoenen").image("kapoenen.png").build()
        val second = branch().id(2).name("Welpen").image("welpen.png").build()
        given(branchRepository.findAllById(listOf(1, 2))).willReturn(listOf(first, second))

        // when
        val result = testee.findSummaries(listOf(1, 2))

        // then
        assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(
            1 to BranchSummary(1, "Kapoenen", "kapoenen.png"),
            2 to BranchSummary(2, "Welpen", "welpen.png")
        ))
        then(branchRepository).should().findAllById(listOf(1, 2))
        then(branchRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun `should omit unknown ids when finding summaries`() {
        // given
        given(branchRepository.findAllById(listOf(1, 99))).willReturn(listOf(branch().id(1).build()))

        // when
        val result = testee.findSummaries(listOf(1, 99))

        // then
        assertThat(result).containsOnlyKeys(1)
    }

    @Test
    fun `should return only active branches when finding branches with calendar`() {
        // given
        val active = branch().id(1).name("Kapoenen").image("kapoenen.png").active().build()
        given(branchRepository.getBranchesWithCalendar()).willReturn(listOf(active))

        // when
        val result = testee.findBranchesWithCalendar()

        // then
        assertThat(result).containsExactly(BranchSummary(1, "Kapoenen", "kapoenen.png"))
    }
}
