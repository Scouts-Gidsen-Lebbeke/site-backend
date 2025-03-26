package be.sgl.backend.util

import be.sgl.backend.entity.Address
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Put all the nasty SGV entities together and don't let the nastiness leak out
data class Lid(
    val id: String, // only used at user creation
    val aangepast: String, // ignored
    val persoonsgegevens: Persoonsgegevens,
    val vgagegevens: Vgagegevens,
    val verbondsgegevens: Verbondsgegevens,
    val gebruikersnaam: String, // only used at user creation
    val adressen: List<Adres>,
    val contacten: List<Contact>,
    val email: String,
    val functies: List<Functie>,
    val groepseigenVelden: Map<String, GroepseigenVelden>,
)

data class LidFuncties(
    val functies: List<Functie>
)

data class Persoonsgegevens(
    val geslacht: String?,
    val gsm: String?,
    val rekeningnummer: String?
)

data class Vgagegevens(
    val voornaam: String, // only used at user creation
    val achternaam: String, // only used at user creation
    val geboortedatum: LocalDate,
    val beperking: Boolean,
    val individueleSteekkaartDatumAangepast: LocalDate, // ignored
    val verhoogdekinderbijslag: Boolean, // ignored
    val verminderdlidgeld: Boolean
)

data class Verbondsgegevens(
    val lidnummer: String,
    val klantnummer: String, // ignored
    val lidgeldbetaald: Boolean, // ignored
    val lidkaartafgedrukt: Boolean // ignored
)

data class Adres(
    val id: String?,
    val land: String,
    val postcode: String,
    val gemeente: String,
    val straat: String,
    val giscode: String?, // ignored
    val nummer: String,
    val bus: String?,
    val telefoon: String?, // ignored
    val postadres: Boolean,
    val omschrijving: String?,
    val status: String?, // ignored
)

fun Adres.asAddress(): Address {
    val address = Address()
    address.externalId = id
    address.street = straat
    address.number = nummer.toInt()
    address.subPremise = bus
    address.zipcode = postcode
    address.town = gemeente
    address.country = land
    address.description = omschrijving
    address.postalAdress = postadres
    return address
}

data class Contact(
    val id: String, // ignored
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

data class Groep(
    val id: String, // ignored
    val groepsnummer: String,
    val naam: String,
    val adressen: List<Adres>?,
    val opgericht: String?,
    val email: String?,
    val website: String?,
    val vrijeInfo: String?
)

data class LidAanvraag(
    val groepsnummer: String,
    val opmerkingen: String?,
    val voornaam: String,
    val achternaam: String,
    val geboortedatum: LocalDate,
    val persoonsgegevens: Persoonsgegevens,
    val email: String,
    val adres: Adres,
    val verminderdlidgeld: Boolean,
)

data class Steekkaart(
    val gegevens: GroepseigenGegevens
)

data class GroepseigenGegevens(
    val waarden: Map<String, String?>
)

fun LocalDate?.asExternalDate() = this?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
