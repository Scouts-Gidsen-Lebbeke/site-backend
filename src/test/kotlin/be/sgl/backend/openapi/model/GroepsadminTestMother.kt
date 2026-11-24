package be.sgl.backend.openapi.model

import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Test mothers for the generated Groepsadmin client models.
 */
object GroepsadminTestMother {

    const val EXTERNAL_ORGANIZATION_ID = "O1234G"

    fun lid() = LidBuilder()

    fun functie() = FunctieInstantieBuilder()

    fun adres() = AdresBuilder()

    fun contact() = ContactBuilder()

    fun groep() = GroepBuilder()

    fun lidAanvraag() = LidAanvraagBuilder()

    fun steekkaart() = SteekkaartBuilder()

    fun ledenlijst() = LedenlijstBuilder()

    fun lijstLid() = LijstLidBuilder()

    class LidBuilder {
        private var id: String? = "externalId"
        private var gebruikersnaam: String? = null
        private var email = "firstName.lastName@corp.com"
        private var voornaam = "firstName"
        private var achternaam = "lastName"
        private var geboortedatum: LocalDate = LocalDate.of(2004, 1, 27)
        private var geslacht = PersoonsGegevens.GeslachtEnum.MAN
        private var gsm: String? = null
        private var rekeningnummer: String? = null
        private var beperking = false
        private var verminderdlidgeld = false
        private var lidnummer: String? = null
        private val adressen = mutableListOf<Adres>()
        private val contacten = mutableListOf<Contact>()
        private val functies = mutableListOf<FunctieInstantie>()

        fun id(id: String?) = apply { this.id = id }

        fun gebruikersnaam(gebruikersnaam: String) = apply { this.gebruikersnaam = gebruikersnaam }

        fun email(email: String) = apply { this.email = email }

        fun voornaam(voornaam: String) = apply { this.voornaam = voornaam }

        fun achternaam(achternaam: String) = apply { this.achternaam = achternaam }

        fun geboortedatum(geboortedatum: LocalDate) = apply { this.geboortedatum = geboortedatum }

        fun geslacht(geslacht: PersoonsGegevens.GeslachtEnum) = apply { this.geslacht = geslacht }

        fun gsm(gsm: String) = apply { this.gsm = gsm }

        fun rekeningnummer(rekeningnummer: String) = apply { this.rekeningnummer = rekeningnummer }

        fun beperking() = apply { this.beperking = true }

        fun verminderdlidgeld() = apply { this.verminderdlidgeld = true }

        fun lidnummer(lidnummer: String?) = apply { this.lidnummer = lidnummer }

        fun adressen(vararg adressen: Adres) = apply { this.adressen.addAll(adressen) }

        fun contacten(vararg contacten: Contact) = apply { this.contacten.addAll(contacten) }

        fun functies(vararg functies: FunctieInstantie) = apply { this.functies.addAll(functies) }

        fun build(): Lid {
            val lid = Lid()
            lid.id = id
            lid.gebruikersnaam = gebruikersnaam
            lid.email = email
            lid.persoonsgegevens = PersoonsGegevens().apply {
                this.geslacht = this@LidBuilder.geslacht
                this.gsm = this@LidBuilder.gsm
                this.rekeningnummer = this@LidBuilder.rekeningnummer
            }
            lid.vgagegevens = VgaGegevens().apply {
                this.voornaam = this@LidBuilder.voornaam
                this.achternaam = this@LidBuilder.achternaam
                this.geboortedatum = this@LidBuilder.geboortedatum
                this.beperking = this@LidBuilder.beperking
                this.verminderdlidgeld = this@LidBuilder.verminderdlidgeld
            }
            lid.verbondsgegevens = VerbondsGegevens().apply { this.lidnummer = this@LidBuilder.lidnummer }
            lid.adressen = adressen.toMutableList()
            lid.contacten = contacten.toMutableList()
            lid.functies = functies.toMutableList()
            return lid
        }
    }

    class FunctieInstantieBuilder {
        private var groep = EXTERNAL_ORGANIZATION_ID
        private var functie = "functionId"
        private var begin: OffsetDateTime = OffsetDateTime.now().minusYears(1)
        private var einde: OffsetDateTime? = null

        fun groep(groep: String) = apply { this.groep = groep }

        fun functie(functie: String) = apply { this.functie = functie }

        fun begin(begin: OffsetDateTime) = apply { this.begin = begin }

        fun ended() = apply { this.einde = OffsetDateTime.now().minusDays(1) }

        fun build(): FunctieInstantie {
            val functieInstantie = FunctieInstantie()
            functieInstantie.groep = groep
            functieInstantie.functie = functie
            functieInstantie.begin = begin
            functieInstantie.einde = einde
            return functieInstantie
        }
    }

    class AdresBuilder {
        private var id: String? = "addressExternalId"
        private var land = "BE"
        private var postcode = "9280"
        private var gemeente = "Lebbeke"
        private var straat = "Kerkstraat"
        private var nummer = "1"
        private var bus: String? = null
        private var telefoon: String? = null
        private var postadres = false
        private var omschrijving: String? = null

        fun id(id: String?) = apply { this.id = id }

        fun land(land: String) = apply { this.land = land }

        fun postcode(postcode: String) = apply { this.postcode = postcode }

        fun gemeente(gemeente: String) = apply { this.gemeente = gemeente }

        fun straat(straat: String) = apply { this.straat = straat }

        fun nummer(nummer: String) = apply { this.nummer = nummer }

        fun bus(bus: String?) = apply { this.bus = bus }

        fun telefoon(telefoon: String) = apply { this.telefoon = telefoon }

        fun postadres() = apply { this.postadres = true }

        fun omschrijving(omschrijving: String) = apply { this.omschrijving = omschrijving }

        fun build(): Adres {
            val adres = Adres()
            adres.id = id
            adres.land = land
            adres.postcode = postcode
            adres.gemeente = gemeente
            adres.straat = straat
            adres.nummer = nummer
            adres.bus = bus
            adres.telefoon = telefoon
            adres.postadres = postadres
            adres.omschrijving = omschrijving
            return adres
        }
    }

    class ContactBuilder {
        private var id: String? = "contactExternalId"
        private var adres = "adresExternalId"
        private var voornaam = "contactFirstName"
        private var achternaam = "contactName"
        private var gsm: String? = null
        private var email: String? = null
        private var rol = Contact.RolEnum.MOEDER

        fun id(id: String?) = apply { this.id = id }

        fun adres(adres: String) = apply { this.adres = adres }

        fun voornaam(voornaam: String) = apply { this.voornaam = voornaam }

        fun achternaam(achternaam: String) = apply { this.achternaam = achternaam }

        fun gsm(gsm: String) = apply { this.gsm = gsm }

        fun email(email: String) = apply { this.email = email }

        fun rol(rol: Contact.RolEnum) = apply { this.rol = rol }

        fun build(): Contact {
            val contact = Contact()
            contact.id = id
            contact.adres = adres
            contact.voornaam = voornaam
            contact.achternaam = achternaam
            contact.gsm = gsm
            contact.email = email
            contact.rol = rol
            return contact
        }
    }

    class GroepBuilder {
        private var id = EXTERNAL_ORGANIZATION_ID
        private var naam = "groupName"
        private var email: String? = null
        private var vrijeInfo: String? = null
        private var adressen: MutableList<Adres>? = mutableListOf(AdresBuilder().build())

        fun naam(naam: String) = apply { this.naam = naam }

        fun email(email: String) = apply { this.email = email }

        fun vrijeInfo(vrijeInfo: String) = apply { this.vrijeInfo = vrijeInfo }

        fun adressen(vararg adressen: Adres) = apply { this.adressen = adressen.toMutableList() }

        fun noAdressen() = apply { this.adressen = null }

        fun build(): Groep {
            val groep = Groep()
            groep.id = id
            groep.groepsnummer = id
            groep.naam = naam
            groep.email = email
            groep.vrijeInfo = vrijeInfo
            groep.adressen = adressen
            return groep
        }
    }

    class LidAanvraagBuilder {
        private var id = "requestId"
        private var voornaam = "firstName"
        private var achternaam = "lastName"
        private var email = "firstName.lastName@corp.com"

        fun id(id: String) = apply { this.id = id }

        fun voornaam(voornaam: String) = apply { this.voornaam = voornaam }

        fun achternaam(achternaam: String) = apply { this.achternaam = achternaam }

        fun email(email: String) = apply { this.email = email }

        fun build(): LidAanvraag {
            val aanvraag = LidAanvraag()
            aanvraag.id = id
            aanvraag.voornaam = voornaam
            aanvraag.achternaam = achternaam
            aanvraag.email = email
            return aanvraag
        }
    }

    class SteekkaartBuilder {
        private val waarden = mutableMapOf<String, String>()

        fun waarde(key: String, value: String) = apply { this.waarden[key] = value }

        fun build(): Steekkaart {
            val steekkaart = Steekkaart()
            steekkaart.gegevens = GroepseigenGegevens().apply { this.waarden = this@SteekkaartBuilder.waarden }
            return steekkaart
        }
    }

    class LedenlijstBuilder {
        private val leden = mutableListOf<LijstLid>()
        private var totaal: Int? = null

        fun leden(vararg leden: LijstLid) = apply { this.leden.addAll(leden) }

        fun totaal(totaal: Int) = apply { this.totaal = totaal }

        fun build(): Ledenlijst {
            val ledenlijst = Ledenlijst()
            ledenlijst.leden = leden.toMutableList()
            ledenlijst.aantal = leden.size
            ledenlijst.totaal = totaal ?: leden.size
            return ledenlijst
        }
    }

    class LijstLidBuilder {
        private var id = "externalId"
        private var voornaam: String? = "firstName"
        private var achternaam: String? = "lastName"
        private var email: String? = "firstName.lastName@corp.com"

        fun id(id: String) = apply { this.id = id }

        fun voornaam(voornaam: String?) = apply { this.voornaam = voornaam }

        fun achternaam(achternaam: String?) = apply { this.achternaam = achternaam }

        fun email(email: String?) = apply { this.email = email }

        fun build(): LijstLid {
            val lijstLid = LijstLid()
            lijstLid.id = id
            val waarden = mutableMapOf<String, String>()
            voornaam?.let { waarden["be.vvksm.groepsadmin.model.column.VoornaamColumn"] = it }
            achternaam?.let { waarden["be.vvksm.groepsadmin.model.column.AchternaamColumn"] = it }
            email?.let { waarden["be.vvksm.groepsadmin.model.column.EmailColumn"] = it }
            lijstLid.waarden = waarden
            return lijstLid
        }
    }
}
