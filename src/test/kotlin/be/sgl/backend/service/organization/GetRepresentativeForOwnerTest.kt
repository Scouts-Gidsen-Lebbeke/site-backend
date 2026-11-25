package be.sgl.backend.service.organization

import be.sgl.backend.entity.setting.SettingId
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import be.sgl.backend.repository.user.UserRepository
import be.sgl.backend.shared.image.ImageService
import be.sgl.backend.shared.image.ImageService.ImageDirectory.ORGANIZATION
import be.sgl.backend.service.SettingService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.io.File

@ExtendWith(MockitoExtension::class)
class GetRepresentativeForOwnerTest {

    @Mock
    lateinit var settingService: SettingService
    @Mock
    lateinit var userRepository: UserRepository
    @Mock
    lateinit var imageService: ImageService
    @InjectMocks
    lateinit var testee: GetRepresentativeForOwner

    private val username = "representative"
    private val defaultTitle = "Vertegenwoordiger"
    private val signatureFileName = "signature.png"
    private val user = user().username(username).build()

    private fun givenRepresentativeUser() {
        given(settingService.get(SettingId.REPRESENTATIVE_USERNAME.name)).willReturn(username)
        given(userRepository.getByUsername(username)).willReturn(user)
    }

    @Test
    fun `should throw when no representative is configured`() {
        // given
        given(settingService.get(SettingId.REPRESENTATIVE_USERNAME.name)).willReturn(null)

        // when / then
        assertThatThrownBy { testee.execute() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessage("No representative configured for organization!")
    }

    @Test
    fun `should throw when no signature is configured`() {
        // given
        givenRepresentativeUser()
        given(settingService.getOrDefault(SettingId.REPRESENTATIVE_TITLE.name, defaultTitle)).willReturn(defaultTitle)
        given(settingService.get(SettingId.REPRESENTATIVE_SIGNATURE.name)).willReturn(null)

        // when / then
        assertThatThrownBy { testee.execute() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessage("No signature configured for organization!")
    }

    @Test
    fun `should throw when the signature image does not exist`() {
        // given
        givenRepresentativeUser()
        given(settingService.getOrDefault(SettingId.REPRESENTATIVE_TITLE.name, defaultTitle)).willReturn(defaultTitle)
        given(settingService.get(SettingId.REPRESENTATIVE_SIGNATURE.name)).willReturn(signatureFileName)
        given(imageService.get(signatureFileName, ORGANIZATION)).willReturn(null)

        // when / then
        assertThatThrownBy { testee.execute() }
            .isInstanceOf(IncompleteConfigurationException::class.java)
            .hasMessage("No valid signature configured for organization!")
    }

    @Test
    fun `should return the representative with its configured title and signature`() {
        // given
        val title = "Voorzitter"
        val signature = File(signatureFileName)
        givenRepresentativeUser()
        given(settingService.getOrDefault(SettingId.REPRESENTATIVE_TITLE.name, defaultTitle)).willReturn(title)
        given(settingService.get(SettingId.REPRESENTATIVE_SIGNATURE.name)).willReturn(signatureFileName)
        given(imageService.get(signatureFileName, ORGANIZATION)).willReturn(signature)

        // when
        val result = testee.execute()

        // then
        assertThat(result.user).isSameAs(user)
        assertThat(result.title).isEqualTo(title)
        assertThat(result.signature).isEqualTo(signature)
    }
}
