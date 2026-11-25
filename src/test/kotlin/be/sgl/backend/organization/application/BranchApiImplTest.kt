package be.sgl.backend.organization.application

import be.sgl.backend.organization.domain.BranchRepository
import be.sgl.backend.organization.domain.BranchTestMother.branch
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
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
}
