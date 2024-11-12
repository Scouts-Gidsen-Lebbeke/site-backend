package be.sgl.backend.util

import be.sgl.backend.entity.Branch
import java.time.LocalDate

// Put all the nasty SGV entities together and don't let the nastiness leak out
data class Lid(
    val id: String,
    val aangepast: String,
    val persoonsgegevens: Persoonsgegevens,
    val vgagegevens: Vgagegevens,
    val verbondsgegevens: Verbondsgegevens,
    val gebruikersnaam: String,
    val adressen: List<Adres>,
    val contacten: List<Contact>,
    val email: String?,
    val functies: List<Functie>,
    val groepseigenVelden: Map<String, GroepseigenVelden>,
) : UserData {
    override val birthdate: LocalDate?
        get() = LocalDate.parse(vgagegevens.geboortedatum)
    override val emailAddress: String?
        get() = email
    override val mobile: String?
        get() = persoonsgegevens.gsm
    override val hasReduction: Boolean
        get() = persoonsgegevens.verminderdlidgeld
    override val branch: Branch?
        get() = null
}

data class Persoonsgegevens(
    val geslacht: String?,
    val gsm: String?,
    val beperking: Boolean,
    val verminderdlidgeld: Boolean,
    val rekeningnummer: String?
)

data class Vgagegevens(val voornaam: String, val achternaam: String, val geboortedatum: String)

data class Verbondsgegevens(
    val lidnummer: String,
    val klantnummer: String,
    val lidgeldbetaald: Boolean,
    val lidkaartafgedrukt: Boolean
)

data class Adres(
    val id: String,
    val land: String,
    val postcode: String,
    val gemeente: String,
    val straat: String,
    val giscode: String?,
    val nummer: String,
    val bus: String?,
    val telefoon: String?,
    val postadres: Boolean,
    val omschrijving: String?,
    val status: String?,
)

data class Contact(
    val id: String,
    val voornaam: String,
    val achternaam: String,
    val rol: String,
    val adresId: String?,
    val gsm: String?,
    val email: String?
)

data class Functie(
    val groep: String,
    val functie: String,
    val begin: String,
    val einde: String?,
)

data class GroepseigenVelden(val waarden: Map<String, String?>)
