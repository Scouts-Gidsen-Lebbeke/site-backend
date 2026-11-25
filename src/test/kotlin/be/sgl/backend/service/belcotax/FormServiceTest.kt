package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationFormTestMother.declarationForm
import be.sgl.backend.dto.organization.RepresentativeTestMother.representative
import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.organization.OrganizationTestMother.organization
import be.sgl.backend.entity.user.ContactTestMother.contact
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.shared.exception.IncompleteConfigurationException
import be.sgl.backend.service.organization.GetRepresentativeForOwner
import be.sgl.backend.service.organization.OrganizationProvider
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.awt.image.BufferedImage
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import javax.imageio.ImageIO

@ExtendWith(MockitoExtension::class)
class FormServiceTest {

    @Mock
    lateinit var organizationProvider: OrganizationProvider
    @Mock
    lateinit var getRepresentativeForOwner: GetRepresentativeForOwner
    @InjectMocks
    lateinit var testee: FormService

    @TempDir
    lateinit var tempDir: Path

    private val owner = organization().owner().name("Scouts Lebbeke").kbo("0417035068")
        .address(address().street("Kerkstraat").number("12").subPremise("A").zipcode("9280").town("Lebbeke").build()).build()
    private val certifier = organization().certifier().name("Certificeerder").kbo("0123456789")
        .address(address().street("Certstraat").number("3").zipcode("1000").town("Brussel").build()).build()

    private fun representativeWithSignature() = representative().title("Groepsleider").signature(
        tempDir.resolve("signature.png").toFile().also { ImageIO.write(BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB), "png", it) }
    ).build()

    private fun text(pdf: ByteArray) = Loader.loadPDF(pdf).use { PDFTextStripper().getText(it) }

    @Test
    fun `should fill the declaration form with organization, child, parent and period data`() {
        // given
        val representative = representativeWithSignature()
        val child = user().name("Janssens").firstName("Jimmy").birthdate(LocalDate.of(2015, 3, 15)).nis("15031512383").build()
        val parent = contact().name("Peeters").firstName("Marie").nis("85031512369").build()
        val activity = activityRegistration().price(100.0)
            .start(LocalDateTime.of(2025, 7, 1, 10, 0)).end(LocalDateTime.of(2025, 7, 10, 12, 0)).build()
        val form = declarationForm().user(child).parent(parent).activity1(activity).build()
        given(organizationProvider.getOwner()).willReturn(owner)
        given(organizationProvider.getCertifier()).willReturn(certifier)
        given(getRepresentativeForOwner.execute()).willReturn(representative)

        // when
        val pdf = testee.createForm(form)

        // then
        assertThat(text(pdf)).contains(
            owner.name, owner.kbo, "12A", certifier.name, certifier.kbo,
            child.name, child.firstName, "15/03/2015",
            parent.name, parent.firstName, parent.nis,
            form.id, form.year,
            "01/07/2025 - 10/07/2025",
            representative.user.getFullName(), representative.title
        )
    }

    @Test
    fun `should fail when no owner organization is configured`() {
        // given
        given(organizationProvider.getOwner()).willAnswer { throw IncompleteConfigurationException("No organization configured!") }

        // when / then
        assertThatThrownBy { testee.createForm(declarationForm().build()) }
            .isInstanceOf(IncompleteConfigurationException::class.java)
    }
}
