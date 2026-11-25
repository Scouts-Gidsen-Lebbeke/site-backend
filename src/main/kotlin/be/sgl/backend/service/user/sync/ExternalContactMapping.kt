package be.sgl.backend.service.user.sync

import be.sgl.backend.people.domain.Contact
import be.sgl.backend.people.domain.ContactRole
import be.sgl.backend.people.domain.User
import be.sgl.backend.openapi.model.Contact as ExternalContact

/**
 * Copies a Groepsadmin contact onto this contact of [user], linking it by external id so later syncs update it.
 * The user's addresses should already be synced, so the contact address can be matched on its external id.
 */
fun Contact.updateFrom(externalContact: ExternalContact, user: User) = apply {
    externalId = externalContact.id
    name = externalContact.achternaam
    firstName = externalContact.voornaam
    this.user = user
    role = when (externalContact.rol) {
        ExternalContact.RolEnum.VADER -> ContactRole.FATHER
        ExternalContact.RolEnum.MOEDER -> ContactRole.MOTHER
        ExternalContact.RolEnum.VOOGD -> ContactRole.GUARDIAN
        else -> ContactRole.RESPONSIBLE
    }
    address = user.addresses.firstOrNull { it.externalId == externalContact.adres }
    mobile = externalContact.gsm
    email = externalContact.email
}
