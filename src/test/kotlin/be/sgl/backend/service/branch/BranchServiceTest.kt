package be.sgl.backend.service.branch

import be.sgl.backend.dto.branch.BranchDTO
import be.sgl.backend.dto.branch.CreateOrUpdateBranchRequestTestMother.branchRequest
import be.sgl.backend.entity.branch.Branch
import be.sgl.backend.entity.branch.BranchStatus
import be.sgl.backend.entity.branch.BranchTestMother.branch
import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.entity.user.Sex
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.BranchNotFoundException
import be.sgl.backend.repository.branch.BranchRepository
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.BRANCH
import be.sgl.backend.shared.image.ImageService.ImageDirectory.TEMPORARY
import be.sgl.backend.service.SettingService
import be.sgl.backend.shared.util.anyValue
import be.sgl.backend.shared.util.argumentCaptor
import be.sgl.backend.shared.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class BranchServiceTest {

    @Mock
    lateinit var branchRepository: BranchRepository
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var imageService: ImageService
    @Mock
    lateinit var settingService: SettingService
    @InjectMocks
    lateinit var testee: BranchService

    private val id = 1

    @Nested
    inner class GetBranch {

        @Test
        fun `should include the staff when staff is visible`() {
            // given
            val branch = branch().build()
            val staff = user().name("staffName").build()
            given(branchRepository.findById(id)).willReturn(Optional.of(branch))
            given(settingService.getOrDefault(SettingId.STAFF_VISIBLE.name, true)).willReturn(true)
            given(userRepository.getStaffForBranch(branch)).willReturn(listOf(staff))

            // when
            val result = testee.getBranchDTOById(id)

            // then
            assertThat(result.staff).extracting<String> { it.name }.containsExactly(staff.name)
        }

        @Test
        fun `should not include the staff when staff is hidden`() {
            // given
            val branch = branch().build()
            given(branchRepository.findById(id)).willReturn(Optional.of(branch))
            given(settingService.getOrDefault(SettingId.STAFF_VISIBLE.name, true)).willReturn(false)

            // when
            val result = testee.getBranchDTOById(id)

            // then
            assertThat(result.staff).isEmpty()
            then(userRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should throw when the branch does not exist`() {
            // given
            given(branchRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.getBranchDTOById(id) }
                .isInstanceOf(BranchNotFoundException::class.java)
                .hasMessage("Branch not found")
        }
    }

    @Test
    fun `should move the image out of the temporary folder when creating a branch`() {
        // given
        val request = branchRequest().build()
        val saved = branch().image(request.image!!).build()
        given(branchRepository.save(anyValue<Branch>())).willReturn(saved)

        // when
        val result = testee.createBranch(request)

        // then
        then(imageService).should().move(request.image!!, TEMPORARY, BRANCH)
        assertThat(result).isEqualTo(BranchDTO.of(saved))
    }

    @Nested
    inner class UpdateBranch {

        @Test
        fun `should throw when the branch does not exist`() {
            // given
            given(branchRepository.findById(id)).willReturn(Optional.empty())

            // when / then
            assertThatThrownBy { testee.updateBranch(id, branchRequest().build()) }
                .isInstanceOf(BranchNotFoundException::class.java)
        }

        @Test
        fun `should copy every field of the request`() {
            // given
            val branch = branch().image("same.jpg").build()
            val request = branchRequest().image("same.jpg").sex(Sex.FEMALE).hidden().build()
            given(branchRepository.findById(id)).willReturn(Optional.of(branch))
            given(branchRepository.save(branch)).willReturn(branch)

            // when
            testee.updateBranch(id, request)

            // then
            assertThat(branch.name).isEqualTo(request.name)
            assertThat(branch.email).isEqualTo(request.email)
            assertThat(branch.minimumAge).isEqualTo(request.minimumAge)
            assertThat(branch.maximumAge).isEqualTo(request.maximumAge)
            assertThat(branch.sex).isEqualTo(Sex.FEMALE)
            assertThat(branch.description).isEqualTo(request.description)
            assertThat(branch.law).isEqualTo(request.law)
            assertThat(branch.status).isEqualTo(BranchStatus.HIDDEN)
            assertThat(branch.staffTitle).isEqualTo(request.staffTitle)
        }

        @Test
        fun `should keep the image when it did not change`() {
            // given
            val branch = branch().image("same.jpg").build()
            given(branchRepository.findById(id)).willReturn(Optional.of(branch))
            given(branchRepository.save(branch)).willReturn(branch)

            // when
            testee.updateBranch(id, branchRequest().image("same.jpg").build())

            // then
            then(imageService).shouldHaveNoInteractions()
        }

        @Test
        fun `should replace the image when it changed`() {
            // given
            val oldImage = "old.jpg"
            val newImage = "new.jpg"
            val branch = branch().image(oldImage).build()
            given(branchRepository.findById(id)).willReturn(Optional.of(branch))
            given(branchRepository.save(branch)).willReturn(branch)

            // when
            testee.updateBranch(id, branchRequest().image(newImage).build())

            // then
            then(imageService).should().delete(BRANCH, oldImage)
            then(imageService).should().move(newImage, TEMPORARY, BRANCH)
            assertThat(branch.image).isEqualTo(newImage)
        }

        @Test
        fun `should save the updated branch`() {
            // given
            val branch = branch().build()
            val captor = argumentCaptor<Branch>()
            given(branchRepository.findById(id)).willReturn(Optional.of(branch))
            given(branchRepository.save(branch)).willReturn(branch)

            // when
            testee.updateBranch(id, branchRequest().image(branch.image).build())

            // then
            then(branchRepository).should().save(captureValue(captor))
            assertThat(captor.value).isSameAs(branch)
            then(imageService).should(never()).delete(anyValue(), anyValue())
        }
    }
}
