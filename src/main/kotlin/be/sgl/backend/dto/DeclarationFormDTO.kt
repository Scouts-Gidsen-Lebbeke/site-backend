package be.sgl.backend.dto

import be.sgl.backend.entity.ActivityRegistration
import be.sgl.backend.entity.Address
import be.sgl.backend.entity.Contact
import be.sgl.backend.entity.User
import java.time.format.DateTimeFormatter

data class DeclarationFormDTO(
    val user: User,
    val activity1: ActivityRegistration,
    val activity2: ActivityRegistration?,
    val activity3: ActivityRegistration?,
    val activity4: ActivityRegistration?
) {
    val year: String
        get() = activity1.start.year.toString()
    val id: String
        get() = "${user.name[0]}${user.firstName[0]}${user.userData.birthdate.format(ID_DATE_FORMAT)}-$year"
    val address: Address
        get() = user.userData.addresses.first { it.postalAdress }
    val parent: Contact
        get() = user.userData.contacts.first { it.nis != null }
}

val ID_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMMyyyy")