package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.Organization
import generated.Verzending
import generated.Verzendingen
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DispatchService {
    fun createDispatch(year: Int, owner: Organization) : Verzendingen {
        val dispatch = Verzending()
        dispatch.v0002Inkomstenjaar = year.toString()
        dispatch.v0010Bestandtype = "BELCOTAX"
        dispatch.v0011Aanmaakdatum = LocalDate.now().asBelcotaxDate()
        dispatch.v0014Naam = owner.name.escaped()
        dispatch.v0015Adres = owner.location.streetAddress

        return Verzendingen().apply { verzending = dispatch }
    }

    private fun LocalDate.asBelcotaxDate(): String = this.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))

    private fun String.escaped(): String = TODO("")
}