package be.sgl.backend.service.membership

import be.sgl.backend.dto.organization.Representative
import be.sgl.backend.entity.PaymentTestMother.membership
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
import java.time.LocalDateTime
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
class CreateCertificateForMembershipTest {

    @Mock
    lateinit var organizationProvider: OrganizationProvider
    @Mock
    lateinit var getRepresentativeForOwner: GetRepresentativeForOwner
    @InjectMocks
    lateinit var testee: CreateCertificateForMembership

    @TempDir
    lateinit var tempDir: Path

    private fun signature(): File {
        val file = tempDir.resolve("signature.png").toFile()
        ImageIO.write(BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB), "png", file)
        return file
    }

    @Test
    fun `should throw when the membership is not paid`() {
        // given
        val membership = membership().paid(false).build()

        // when / then
        assertThatThrownBy { testee.execute(membership) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("A certificate can only be generated for a paid membership!")
        then(organizationProvider).shouldHaveNoInteractions()
        then(getRepresentativeForOwner).shouldHaveNoInteractions()
    }

    @Test
    fun `should fill the membership form when the membership is paid`() {
        // given
        val member = user().name("Janssens").firstName("Pieter").build()
        val membership = membership().user(member).paid(true).price(45.0).createdDate(LocalDateTime.of(2025, 9, 10, 10, 0)).build()
        val signatory = user().name("Peeters").firstName("An").build()
        given(organizationProvider.getOwner()).willReturn(organization().name("Scouts Lebbeke").email("info@group.com").build())
        given(getRepresentativeForOwner.execute()).willReturn(Representative(signatory, "Voorzitter", signature()))

        // when
        val certificate = testee.execute(membership)

        // then
        val text = Loader.loadPDF(certificate).use { PDFTextStripper().getText(it) }
        assertThat(text).contains(member.name, member.firstName, signatory.getFullName(), "Scouts Lebbeke", "10/09/2025")
    }
}
