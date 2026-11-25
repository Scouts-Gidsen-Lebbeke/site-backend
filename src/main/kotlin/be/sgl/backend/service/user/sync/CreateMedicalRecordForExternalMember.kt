package be.sgl.backend.service.user.sync

import be.sgl.backend.people.domain.BloodGroup
import be.sgl.backend.people.domain.MedicalRecord
import be.sgl.backend.people.domain.User
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.people.domain.MedicalRecordRepository
import mu.KotlinLogging

@ExternalUsecase
class CreateMedicalRecordForExternalMember(
    private val ledenApi: LedenApi,
    private val medicalRecordRepository: MedicalRecordRepository
) {

    private val logger = KotlinLogging.logger {}

    fun execute(user: User) {
        logger.info { "Translating medical record..." }
        val externalId = user.externalId ?: run {
            logger.info { "User is not linked to Groepsadmin, no medical record to translate" }
            return
        }
        val externalMedicalRecord = ledenApi.getLidSteekkaart(externalId, null).gegevens
        val medicalRecord = MedicalRecord().apply {
            this.user = user
            mayBePhotographed = externalMedicalRecord.waarden["d5f75e1e463384de014639190ebb00eb"] == "ja"
            mayTakePainkillers = externalMedicalRecord.waarden["d5f75e1e463384de0146390e395900e2"] == "ja"
            // Zo ja, op vlak van voeding (vb. vegetariër, halal):
            foodAnomalies = externalMedicalRecord.waarden["d5f75e1e463384de0146391a3b4800ed"].sanitized()
            // Onze zoon of dochter moet een bepaald dieet volgen
            allergies = externalMedicalRecord.waarden["d5f75e1e463384de0146391124af00e5"].sanitized()
            val impossibleActivities = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f119f740016"].sanitized()
            val atSports = externalMedicalRecord.waarden["d5f75e1e480b9aa901480c7fb70100de"].sanitized()
            val atHygiene = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f1464ef001a"].sanitized()
            val atSocial = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f14d9de001b"].sanitized()
            val other = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f1523c6001c"].sanitized()
            activityRestrictions = listOfNotNull(impossibleActivities, atSports, atHygiene, atSocial, other).joinToString(",").sanitized()
            familyRemarks = externalMedicalRecord.waarden["d5f75e1e4610ed0201461f026f8e0013"].sanitized()
            // Zo ja, andere aandachtspunten die belang kunnen hebben bij de omgang met ons kind:
            socialRemarks = externalMedicalRecord.waarden["d5f75e1e463384de0146391abdd000ee"].sanitized()
            val diseaseList = externalMedicalRecord.waarden["d5f75e1e463384de01463905280100de"].sanitized()
            // TODO: also read the disease guidance once its Groepsadmin key is known (it used to read the hygiene key)
            diseases = diseaseList
            // ja/nee, but no concrete info
            medications = externalMedicalRecord.waarden["d5f75e1e463384de01463901e13c00dc"].sanitized()
            // physician field not derivable
            physicianContact = externalMedicalRecord.waarden["d5f75e1e463384de0146391800f100e9"].sanitized()
            bloodGroup = when(externalMedicalRecord.waarden["d5f75e1e463384de01463916d21b00e8"]) {
                "O+" -> BloodGroup.OP
                "O-" -> BloodGroup.ON
                "A+" -> BloodGroup.AP
                "A-" -> BloodGroup.AN
                "B+" -> BloodGroup.BP
                "B-" -> BloodGroup.BN
                "AB+" -> BloodGroup.ABP
                "AB-" -> BloodGroup.ABN
                else -> BloodGroup.UNKNOWN
            }
        }
        medicalRecordRepository.save(medicalRecord)
        logger.info { "Medical record created" }
    }

    private fun String?.sanitized(): String? {
        return this?.takeIf { it.isNotBlank() && it != "/" && !it.equals("nee", true)
                && !it.equals("neen", true) && !it.equals("nvt", true)
                && !it.equals("geen", true) }
    }
}