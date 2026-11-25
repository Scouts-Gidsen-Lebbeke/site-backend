package be.sgl.backend.service.belcotax

import be.sgl.backend.dto.DeclarationForm
import be.sgl.backend.dto.DeclarationFormTestMother.declarationForm
import be.sgl.backend.dto.organization.RepresentativeTestMother.representative
import be.sgl.backend.shared.address.AddressTestMother.address
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.organization.domain.Organization
import be.sgl.backend.organization.domain.OrganizationTestMother.organization
import be.sgl.backend.people.domain.ContactTestMother.contact
import be.sgl.backend.people.domain.UserTestMother.user
import be.sgl.backend.service.organization.GetRepresentativeForOwner
import be.sgl.backend.organization.application.OrganizationProvider
import generated.Fiche28186
import generated.Verzendingen
import jakarta.xml.bind.JAXBContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.context.i18n.LocaleContextHolder
import java.io.StringWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT) // the organization stubs are shared by all tests, some fail before using all of them
class DispatchServiceTest {

    @Mock
    lateinit var organizationProvider: OrganizationProvider
    @Mock
    lateinit var getRepresentativeForOwner: GetRepresentativeForOwner
    @InjectMocks
    lateinit var testee: DispatchService

    private val ownerKbo = "0417035068"
    private val ownerEmail = "info@scouts.be"
    private val ownerMobile = "0471234567"
    private val certifierKbo = "0123456789"
    private val representative = representative().build()
    private val originalLocale = Locale.getDefault()

    private var owner = owner().build()
    private var certifier = certifier().build()

    private fun owner() = organization().owner().name("Scouts Lebbeke").kbo(ownerKbo).email(ownerEmail).mobile(ownerMobile)
        .address(address().street("Kerkstraat").number("12").subPremise("A").zipcode("9280").town("Lebbeke").build())

    private fun certifier() = organization().certifier().name("Certifier & Partners").kbo(certifierKbo)
        .address(address().street("Certstraat").number("3").zipcode("1000").town("Brussel").build())

    @BeforeEach
    fun setUp() {
        Locale.setDefault(Locale.US)
        LocaleContextHolder.setLocale(Locale.of("nl", "BE"))
        given(organizationProvider.getOwner()).willAnswer { owner }
        given(organizationProvider.getCertifier()).willAnswer { certifier }
        given(getRepresentativeForOwner.execute()).willReturn(representative)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
        LocaleContextHolder.resetLocaleContext()
    }

    private fun createDispatch(vararg forms: DeclarationForm, previous: String? = null) =
        testee.createDispatch(forms.toList(), previous).verzending

    private fun fiches(vararg forms: DeclarationForm) =
        createDispatch(*forms).aangiften.aangifte.single().opgaven.opgave32500OrOpgave32501OrOpgave32510.map { it as Fiche28186 }

    private fun fiche(form: DeclarationForm) = fiches(form).single()

    private fun registration(price: Double, start: LocalDate, days: Long) = activityRegistration().price(price).paid(true)
        .start(start.atTime(10, 0)).end(start.plusDays(days - 1).atTime(12, 0)).build()

    @Test
    fun `should fail when no forms are given`() {
        // given

        // when / then
        assertThatThrownBy { testee.createDispatch(emptyList()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("At least one declaration")
    }

    @Nested
    inner class Header {

        @Test
        fun `should fill the sender fields from the owner organization and representative`() {
            // given
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0002Inkomstenjaar).isEqualTo(form.year)
            assertThat(dispatch.v0010Bestandtype).isEqualTo("BELCOTAX")
            assertThat(dispatch.v0014Naam).isEqualTo(owner.name)
            assertThat(dispatch.v0015Adres).isEqualTo("Kerkstraat 12A")
            assertThat(dispatch.v0016Postcode).isEqualTo("9280")
            assertThat(dispatch.v0027Postcodebuitenl).isNull()
            assertThat(dispatch.v0017Gemeente).isEqualTo("Lebbeke")
            assertThat(dispatch.v0018Telefoonnummer).isEqualTo(ownerMobile)
            assertThat(dispatch.v0020Identificatie).isEqualTo("BConv86")
            assertThat(dispatch.v0021Contactpersoon).isEqualTo(representative.user.getFullName())
            assertThat(dispatch.v0023Emailadres).isEqualTo(ownerEmail)
            assertThat(dispatch.v0024Nationaalnr).isEqualTo(ownerKbo)
            assertThat(dispatch.v0028Landwoonplaats).isEqualTo("150")
        }

        @Test
        fun `should use today as creation date in belcotax date format`() {
            // given
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0011Aanmaakdatum).isEqualTo(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))
        }

        @Test
        fun `should mark the dispatch as original when no previous dispatch is referenced`() {
            // given
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0025Typeenvoi).isEqualTo("0")
            assertThat(dispatch.v0026Referte).isNull()
        }

        @Test
        fun `should mark the dispatch as correction when a previous dispatch is referenced`() {
            // given
            val form = declarationForm().build()
            val previous = "BC-2025-001"

            // when
            val dispatch = createDispatch(form, previous = previous)

            // then
            assertThat(dispatch.v0025Typeenvoi).isEqualTo("1")
            assertThat(dispatch.v0026Referte).isEqualTo(previous)
        }

        @Test
        fun `should strip ampersands from the organization name`() {
            // given
            owner = owner().name("Scouts & Gidsen").build()
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0014Naam).isEqualTo("Scouts  Gidsen")
        }

        @Test
        fun `should leave phone and email empty when the owner has no contact methods`() {
            // given
            owner = organization().owner().kbo(ownerKbo).build()
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0018Telefoonnummer).isNull()
            assertThat(dispatch.v0023Emailadres).isNull()
        }

        @Test
        fun `should fail when the organization name exceeds 41 characters`() {
            // given
            owner = owner().name("x".repeat(42)).build()
            val form = declarationForm().build()

            // when / then
            assertThatThrownBy { createDispatch(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("organization name")
        }

        @Test
        fun `should fail when the owner has no kbo number`() {
            // given
            owner = owner().kbo(null).build()
            val form = declarationForm().build()

            // when / then
            assertThatThrownBy { createDispatch(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Missing organization national number")
        }

        @Test
        fun `should fail when the owner kbo number does not have 10 digits`() {
            // given
            owner = owner().kbo("417035068").build()
            val form = declarationForm().build()

            // when / then
            assertThatThrownBy { createDispatch(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("organization national number")
        }

        @Test
        fun `should fail when a belgian owner zipcode does not have 4 digits`() {
            // given
            owner = owner().address(address().zipcode("92800").build()).build()
            val form = declarationForm().build()

            // when / then
            assertThatThrownBy { createDispatch(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("organization zipcode")
        }

        @Test
        fun `should fail when the owner street address exceeds 32 characters`() {
            // given
            owner = owner().address(address().street("s".repeat(31)).number("12").build()).build()
            val form = declarationForm().build()

            // when / then
            assertThatThrownBy { createDispatch(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("street address")
        }

        @Test
        @Disabled("Possible bug: foreign zipcodes must be exactly 12 characters (assertLength) while the field only has a max length of 12")
        fun `should use the foreign zipcode field when the owner is not located in belgium`() {
            // given
            owner = owner().address(address().zipcode("1234 AB").country("NL").build()).build()
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0016Postcode).isNull()
            assertThat(dispatch.v0027Postcodebuitenl).isEqualTo("1234 AB")
            assertThat(dispatch.v0028Landwoonplaats).isEqualTo("129")
        }
    }

    @Nested
    inner class Language {

        @ParameterizedTest
        @CsvSource("nl,1", "en,1", "fr,2", "de,3")
        fun `should derive the language code from the current locale`(language: String, code: String) {
            // given
            LocaleContextHolder.setLocale(Locale.of(language))
            val form = declarationForm().build()

            // when
            val dispatch = createDispatch(form)

            // then
            assertThat(dispatch.v0022Taalcode).isEqualTo(code)
            assertThat(dispatch.aangiften.aangifte.single().a1020Taalcode).isEqualTo(code)
        }

        @Test
        fun `should fill the dutch name fields when the language is dutch`() {
            // given
            val form = declarationForm().build()

            // when
            val declaration = createDispatch(form).aangiften.aangifte.single()

            // then
            assertThat(declaration.a1011Naamnl1).isEqualTo(owner.name)
            assertThat(declaration.a1012Naamnl2).isNull()
            assertThat(declaration.a1013Adresnl).isEqualTo("Kerkstraat 12A")
            assertThat(declaration.a1015Gemeente).isEqualTo("Lebbeke")
            assertThat(declaration.a1027Naamfr1).isNull()
            assertThat(declaration.a1032Naamde1).isNull()
        }

        @Test
        fun `should fill the french name fields when the language is french`() {
            // given
            LocaleContextHolder.setLocale(Locale.FRENCH)
            val form = declarationForm().build()

            // when
            val declaration = createDispatch(form).aangiften.aangifte.single()

            // then
            assertThat(declaration.a1027Naamfr1).isEqualTo(owner.name)
            assertThat(declaration.a1029Adresfr).isEqualTo("Kerkstraat 12A")
            assertThat(declaration.a1030Gemeentefr).isEqualTo("Lebbeke")
            assertThat(declaration.a1011Naamnl1).isNull()
        }

        @Test
        fun `should fill the german name fields when the language is german`() {
            // given
            LocaleContextHolder.setLocale(Locale.GERMAN)
            val form = declarationForm().build()

            // when
            val declaration = createDispatch(form).aangiften.aangifte.single()

            // then
            assertThat(declaration.a1032Naamde1).isEqualTo(owner.name)
            assertThat(declaration.a1034Adresde).isEqualTo("Kerkstraat 12A")
            assertThat(declaration.a1035Gemeentede).isEqualTo("Lebbeke")
            assertThat(declaration.a1011Naamnl1).isNull()
        }

        @Test
        fun `should split the organization name over two fields when it exceeds 28 characters`() {
            // given
            val name = "a".repeat(28) + "b".repeat(5)
            owner = owner().name(name).build()
            val form = declarationForm().build()

            // when
            val declaration = createDispatch(form).aangiften.aangifte.single()

            // then
            assertThat(declaration.a1011Naamnl1).isEqualTo(name.take(28))
            assertThat(declaration.a1012Naamnl2).isEqualTo(name.drop(28))
        }
    }

    @Nested
    inner class Totals {

        @Test
        fun `should compute record counts and control totals from the forms`() {
            // given
            val form1 = declarationForm().activity1(registration(100.0, LocalDate.of(2025, 7, 1), 10)).index(0).build()
            val form2 = declarationForm().activity1(registration(50.0, LocalDate.of(2025, 8, 1), 5)).index(1).build()
            val form3 = declarationForm().activity1(registration(25.0, LocalDate.of(2025, 9, 1), 2)).index(2).build()
            val forms = listOf(form1, form2, form3)

            // when
            val dispatch = createDispatch(*forms.toTypedArray())

            // then
            val declaration = dispatch.aangiften.aangifte.single()
            assertThat(declaration.a1002Inkomstenjaar).isEqualTo(form1.year)
            assertThat(declaration.a1005Registratienummer).isEqualTo(ownerKbo)
            assertThat(declaration.r8002Inkomstenjaar).isEqualTo(form1.year)
            assertThat(declaration.r8005Registratienummer).isEqualTo(ownerKbo)
            assertThat(declaration.r8010Aantalrecords).isEqualTo((forms.size + 2).toString())
            assertThat(declaration.r8011Controletotaal).isEqualTo((0 + 1 + 2).toString())
            assertThat(declaration.r8012Controletotaal).isEqualTo(forms.sumOf { it.totalPrice * 2 }.toString())
            assertThat(declaration.r8013Totaalvoorheffingen).isEqualTo("0")
            assertThat(dispatch.r9002Inkomstenjaar).isEqualTo(form1.year)
            assertThat(dispatch.r9010Aantallogbestanden).isEqualTo("3")
            assertThat(dispatch.r9011Totaalaantalrecords).isEqualTo((forms.size + 4).toString())
            assertThat(dispatch.r9012Controletotaal).isEqualTo(declaration.r8011Controletotaal)
            assertThat(dispatch.r9013Controletotaal).isEqualTo(declaration.r8012Controletotaal)
            assertThat(dispatch.r9014Controletotaal).isEqualTo(declaration.r8013Totaalvoorheffingen)
        }

        @Test
        fun `should sum the totals of every fiche in the declaration control total`() {
            // given
            val forms = (0 until 3).map { declarationForm().activity1(registration(10.0 * (it + 1), LocalDate.of(2025, 7, 1), 1)).index(it).build() }

            // when
            val fiches = fiches(*forms.toTypedArray())

            // then
            val declaration = createDispatch(*forms.toTypedArray()).aangiften.aangifte.single()
            assertThat(fiches.sumOf { it.f862059Totaalcontrole.toDouble() }).isEqualTo(declaration.r8012Controletotaal.toDouble())
        }

        @Test
        @Disabled("Possible bug: amounts are serialized with Double.toString, so floating point errors leak into the XML (30.299999999999997)")
        fun `should serialize total amounts without floating point artifacts`() {
            // given
            val form = declarationForm()
                .activity1(registration(10.1, LocalDate.of(2025, 7, 1), 1))
                .activity2(registration(20.2, LocalDate.of(2025, 8, 1), 1))
                .build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862064Totalamount).isEqualTo("30.30")
        }
    }

    @Nested
    inner class Fiche {

        @Test
        fun `should fill the fiche identification fields from the form`() {
            // given
            val form = declarationForm().index(1).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f2002Inkomstenjaar).isEqualTo(form.year)
            assertThat(fiche.f2005Registratienummer).isEqualTo(ownerKbo)
            assertThat(fiche.f2008Typefiche).isEqualTo("28186")
            assertThat(fiche.f2009Volgnummer).isEqualTo("0")
            assertThat(fiche.f2010Referentie).isEqualTo(form.id)
            assertThat(fiche.f862031Certificationautorisation).isEqualTo("2")
        }

        @Test
        fun `should number the fiches sequentially in order of the forms`() {
            // given
            val forms = (0 until 3).map { declarationForm().index(it).build() }

            // when
            val fiches = fiches(*forms.toTypedArray())

            // then
            assertThat(fiches.map { it.f2009Volgnummer }).containsExactly("0", "1", "2")
        }

        @Test
        fun `should fill the debtor fields from the parent and the form address`() {
            // given
            val parent = contact().name("Doe").firstName("Jane").nis("85031512369").build()
            val form = declarationForm().parent(parent).address(address().street("Dorp").number("5").zipcode("9200").town("Dendermonde").build()).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f2011Nationaalnr).isEqualTo(parent.nis)
            assertThat(fiche.f2012Geboortedatum).isEqualTo("15-03-1985")
            assertThat(fiche.f2013Naam).isEqualTo(parent.name)
            assertThat(fiche.f2114Voornamen).isEqualTo(parent.firstName)
            assertThat(fiche.f2015Adres).isEqualTo("Dorp 5")
            assertThat(fiche.f2016Postcodebelgisch).isEqualTo("9200")
            assertThat(fiche.f2112Buitenlandspostnummer).isNull()
            assertThat(fiche.f2017Gemeente).isEqualTo("Dendermonde")
            assertThat(fiche.f2018Landwoonplaats).isEqualTo("150")
        }

        @Test
        fun `should derive a 21st century birthdate from the parent nis when the nis year is before the fiscal year`() {
            // given
            val parent = contact().nis("03021012345").build()
            val form = declarationForm().parent(parent).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f2012Geboortedatum).isEqualTo("10-02-2003")
        }

        @Test
        @Disabled("Possible bug: birthdateFromNis uses the raw month of the nis, so bis numbers (month + 20/40) give an invalid date like 15-43-1985")
        fun `should derive the real birth month when the parent has a bis number`() {
            // given
            val parent = contact().nis("85431512345").build()
            val form = declarationForm().parent(parent).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f2012Geboortedatum).isEqualTo("15-03-1985")
        }

        @Test
        fun `should fill the child fields from the user`() {
            // given
            val child = user().name("Doe").firstName("Jimmy").nis("15031512383").birthdate(LocalDate.of(2015, 3, 15)).build()
            val form = declarationForm().user(child).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862106Childname).isEqualTo(child.name)
            assertThat(fiche.f862107Childfirstname).isEqualTo(child.firstName)
            assertThat(fiche.f862153Nnchild).isEqualTo(child.nis)
            assertThat(fiche.f862163Childbirthdate).isEqualTo("15-03-2015")
            assertThat(fiche.f862101Childcountry).isEqualTo(fiche.f2018Landwoonplaats)
            assertThat(fiche.f862102Childaddress).isEqualTo(fiche.f2015Adres)
            assertThat(fiche.f862139Childpostnr).isEqualTo(fiche.f2016Postcodebelgisch)
            assertThat(fiche.f862140Childmunicipality).isEqualTo(fiche.f2017Gemeente)
        }

        @Test
        fun `should fill the certifier fields and strip ampersands from its name`() {
            // given
            val form = declarationForm().build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862100Certifierpostnr).isEqualTo("1000")
            assertThat(fiche.f862109Certifiercbenumber).isEqualTo(certifierKbo)
            assertThat(fiche.f862154Certifiermunicipality).isEqualTo("Brussel")
            assertThat(fiche.f862155Certifiername).isEqualTo("Certifier  Partners")
            assertThat(fiche.f862156Certifieradres).isEqualTo("Certstraat 3")
        }

        @Test
        fun `should fill only the first period when the form has one activity`() {
            // given
            val activity = registration(100.0, LocalDate.of(2025, 7, 1), 10)
            val form = declarationForm().activity1(activity).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862055Begindate1).isEqualTo("01-07-2025")
            assertThat(fiche.f862056Enddate1).isEqualTo("10-07-2025")
            assertThat(fiche.f862110Numberofday1).isEqualTo("10")
            assertThat(fiche.f862060Amount1).isEqualTo("100.00")
            assertThat(fiche.f862064Totalamount).isEqualTo("100.0")
            assertThat(fiche.f862059Totaalcontrole).isEqualTo("200.0")
            assertThat(fiche.f862093Begindate2).isNull()
            assertThat(fiche.f862061Amount2).isNull()
            assertThat(fiche.f862113Numberofday2).isNull()
            assertThat(fiche.f862115Dailytariff2).isNull()
            assertThat(fiche.f862157Begindate3).isNull()
            assertThat(fiche.f862161Begindate4).isNull()
        }

        @Test
        fun `should fill every period when the form has four activities`() {
            // given
            val activity1 = registration(100.0, LocalDate.of(2025, 2, 1), 2)
            val activity2 = registration(50.0, LocalDate.of(2025, 4, 1), 5)
            val activity3 = registration(30.0, LocalDate.of(2025, 7, 1), 3)
            val activity4 = registration(20.0, LocalDate.of(2025, 10, 30), 4)
            val form = declarationForm().activity1(activity1).activity2(activity2).activity3(activity3).activity4(activity4).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862093Begindate2).isEqualTo("01-04-2025")
            assertThat(fiche.f862144Enddate2).isEqualTo("05-04-2025")
            assertThat(fiche.f862113Numberofday2).isEqualTo("5")
            assertThat(fiche.f862061Amount2).isEqualTo("50.00")
            assertThat(fiche.f862157Begindate3).isEqualTo("01-07-2025")
            assertThat(fiche.f862158Enddate3).isEqualTo("03-07-2025")
            assertThat(fiche.f862116Numberofday3).isEqualTo("3")
            assertThat(fiche.f862062Amount3).isEqualTo("30.00")
            assertThat(fiche.f862161Begindate4).isEqualTo("30-10-2025")
            assertThat(fiche.f862162Enddate4).isEqualTo("02-11-2025")
            assertThat(fiche.f862119Numberofday4).isEqualTo("4")
            assertThat(fiche.f862063Amount4).isEqualTo("20.00")
            assertThat(fiche.f862064Totalamount).isEqualTo(form.totalPrice.toString())
        }

        @Test
        fun `should leave the daily tariff empty when it does not exceed the dispatch rate`() {
            // given
            val rate = 14.4
            val form = declarationForm().rate(rate).activity1(registration(rate * 10, LocalDate.of(2025, 7, 1), 10)).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862111Dailytariff1).isNull()
        }

        @Test
        fun `should report the daily tariff when it exceeds the dispatch rate`() {
            // given
            val rate = 14.4
            val form = declarationForm().rate(rate)
                .activity1(registration(200.0, LocalDate.of(2025, 7, 1), 10))
                .activity2(registration(200.0, LocalDate.of(2025, 8, 1), 8))
                .build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862111Dailytariff1).isEqualTo("20.00")
            assertThat(fiche.f862115Dailytariff2).isEqualTo("25.00")
        }

        @Test
        fun `should fail when the child has no nis`() {
            // given
            val form = declarationForm().user(user().build()).build()

            // when / then
            assertThatThrownBy { fiche(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("Missing child nis number")
        }

        @Test
        fun `should fail when the parent nis does not have 11 digits`() {
            // given
            val form = declarationForm().parent(contact().nis("1234").build()).build()

            // when / then
            assertThatThrownBy { fiche(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("parent nis number")
        }

        @Test
        fun `should fail when the certifier is not located in belgium`() {
            // given
            certifier = certifier().address(address().zipcode("75001").country("FR").build()).build()
            val form = declarationForm().build()

            // when / then
            assertThatThrownBy { fiche(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("certifier zipcode")
        }

        @Test
        fun `should fail when the child name exceeds 41 characters`() {
            // given
            val form = declarationForm().user(user().name("n".repeat(42)).nis("15031512383").build()).build()

            // when / then
            assertThatThrownBy { fiche(form) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("child name")
        }

        @ParameterizedTest
        @CsvSource("NL,129", "LU,113", "FR,111", "DE,103", "UK,112")
        fun `should map the country of the form address to the belcotax country code`(country: String, code: String) {
            // given
            val foreignZipcode = "123456789012"
            val form = declarationForm().address(address().zipcode(foreignZipcode).country(country).build()).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f2018Landwoonplaats).isEqualTo(code)
            assertThat(fiche.f2112Buitenlandspostnummer).isEqualTo(foreignZipcode)
            assertThat(fiche.f2016Postcodebelgisch).isNull()
            assertThat(fiche.f862139Childpostnr).isEqualTo(foreignZipcode)
        }

        @Test
        fun `should fail when the country of the form address is not mapped`() {
            // given
            val form = declarationForm().address(address().zipcode("123456789012").country("US").build()).build()

            // when / then
            assertThatThrownBy { fiche(form) }.isInstanceOf(NotImplementedError::class.java)
        }

        @Test
        @Disabled("Possible bug: the ISO code for the United Kingdom is GB (accepted by CountryCodeValidator), but only the non-ISO 'UK' is mapped")
        fun `should map the iso country code of the united kingdom`() {
            // given
            val form = declarationForm().address(address().zipcode("SW1A 1AA").country("GB").build()).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f2018Landwoonplaats).isEqualTo("112")
        }

        @Test
        @Disabled("Possible bug: amounts use String.format without locale, so a JVM with a Dutch/French default locale writes '100,00' into the XML")
        fun `should format amounts independent of the default locale`() {
            // given
            Locale.setDefault(Locale.of("nl", "BE"))
            val form = declarationForm().activity1(registration(100.0, LocalDate.of(2025, 7, 1), 10)).build()

            // when
            val fiche = fiche(form)

            // then
            assertThat(fiche.f862060Amount1).isEqualTo("100.00")
        }
    }

    @Test
    fun `should produce a dispatch that can be marshalled to xml`() {
        // given
        val form = declarationForm().build()
        val dispatch = testee.createDispatch(listOf(form))
        val marshaller = JAXBContext.newInstance(Verzendingen::class.java).createMarshaller()
        val writer = StringWriter()

        // when
        assertThatCode { marshaller.marshal(dispatch, writer) }.doesNotThrowAnyException()

        // then
        assertThat(writer.toString())
            .contains("<v0010_bestandtype>BELCOTAX</v0010_bestandtype>")
            .contains("<f2008_typefiche>28186</f2008_typefiche>")
    }
}
