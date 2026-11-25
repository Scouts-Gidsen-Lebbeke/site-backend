package be.sgl.backend.service.registrable.activity

import be.sgl.backend.dto.organization.Representative
import be.sgl.backend.entity.PayableTestMother.activity
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.organization.domain.OrganizationTestMother.organization
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.service.organization.GetRepresentativeForOwner
import be.sgl.backend.organization.application.OrganizationProvider
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
class CreateCertificateForActivityRegistrationTest {

    @Mock
    lateinit var organizationProvider: OrganizationProvider
    @Mock
    lateinit var getRepresentativeForOwner: GetRepresentativeForOwner
    @InjectMocks
    lateinit var testee: CreateCertificateForActivityRegistration

    @Test
    fun `should fail when the registration is not completed`() {
        // given
        val registration = activityRegistration().completed(false).build()

        // when / then
        assertThatThrownBy { testee.execute(registration) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("A certificate can only be generated for a completed activity!")
        then(organizationProvider).shouldHaveNoInteractions()
    }

    @Test
    fun `should generate a filled participation certificate when the registration is completed`(@TempDir tempDir: Path) {
        // given
        val member = user().name("Peeters").firstName("Jan").build()
        val registration = activityRegistration().completed(true).user(member)
            .activity(activity().name("Zomerkamp").build()).price(150.0).build()
        val signature = File(tempDir.toFile(), "signature.png")
        ImageIO.write(BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", signature)
        given(organizationProvider.getOwner()).willReturn(organization().name("Scouts Lebbeke").build())
        given(getRepresentativeForOwner.execute()).willReturn(Representative(user().build(), "Voorzitter", signature))

        // when
        val pdf = testee.execute(registration)

        // then
        val text = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }
        assertThat(text).contains(member.firstName, member.name, registration.subscribable.name)
    }
}
