package be.sgl.backend.people.domain

import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

object MedicalRecordTestMother {

    fun medicalRecord() = MedicalRecordBuilder()

    class MedicalRecordBuilder {
        private var id: Int? = 1
        private var user = UserTestMother.user().build()
        private var mayBePhotographed = false
        private var mayTakePainkillers = false
        private var foodAnomalies: String? = null
        private var allergies: String? = null
        private var activityRestrictions: String? = null
        private var familyRemarks: String? = null
        private var socialRemarks: String? = null
        private var diseases: String? = null
        private var medications: String? = null
        private var physician: String? = null
        private var physicianContact: String? = null
        private var bloodGroup = BloodGroup.UNKNOWN
        private var lastModifiedDate: LocalDateTime? = LocalDateTime.now()

        fun id(id: Int) = apply { this.id = id }

        fun user(user: User) = apply { this.user = user }

        fun mayBePhotographed() = apply { this.mayBePhotographed = true }

        fun mayTakePainkillers() = apply { this.mayTakePainkillers = true }

        fun foodAnomalies(foodAnomalies: String) = apply { this.foodAnomalies = foodAnomalies }

        fun allergies(allergies: String) = apply { this.allergies = allergies }

        fun activityRestrictions(activityRestrictions: String) = apply { this.activityRestrictions = activityRestrictions }

        fun familyRemarks(familyRemarks: String) = apply { this.familyRemarks = familyRemarks }

        fun socialRemarks(socialRemarks: String) = apply { this.socialRemarks = socialRemarks }

        fun diseases(diseases: String) = apply { this.diseases = diseases }

        fun medications(medications: String) = apply { this.medications = medications }

        fun physician(physician: String) = apply { this.physician = physician }

        fun physicianContact(physicianContact: String) = apply { this.physicianContact = physicianContact }

        fun bloodGroup(bloodGroup: BloodGroup) = apply { this.bloodGroup = bloodGroup }

        fun lastModifiedDate(lastModifiedDate: LocalDateTime?) = apply { this.lastModifiedDate = lastModifiedDate }

        fun build(): MedicalRecord {
            val record = MedicalRecord()
            // id is a JPA-managed val (@MapsId), so it can only be set reflectively
            ReflectionTestUtils.setField(record, "id", id)
            record.user = user
            record.mayBePhotographed = mayBePhotographed
            record.mayTakePainkillers = mayTakePainkillers
            record.foodAnomalies = foodAnomalies
            record.allergies = allergies
            record.activityRestrictions = activityRestrictions
            record.familyRemarks = familyRemarks
            record.socialRemarks = socialRemarks
            record.diseases = diseases
            record.medications = medications
            record.physician = physician
            record.physicianContact = physicianContact
            record.bloodGroup = bloodGroup
            record.lastModifiedDate = lastModifiedDate
            return record
        }
    }
}
