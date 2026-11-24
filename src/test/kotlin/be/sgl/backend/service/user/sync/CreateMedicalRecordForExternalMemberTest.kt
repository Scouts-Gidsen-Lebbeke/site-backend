package be.sgl.backend.service.user.sync

import be.sgl.backend.entity.user.BloodGroup
import be.sgl.backend.entity.user.MedicalRecord
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.openapi.api.LedenApi
import be.sgl.backend.openapi.model.GroepsadminTestMother.steekkaart
import be.sgl.backend.repository.user.MedicalRecordRepository
import be.sgl.backend.util.argumentCaptor
import be.sgl.backend.util.captureValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CreateMedicalRecordForExternalMemberTest {

    @Mock
    lateinit var ledenApi: LedenApi
    @Mock
    lateinit var medicalRecordRepository: MedicalRecordRepository
    @InjectMocks
    lateinit var testee: CreateMedicalRecordForExternalMember

    private val user = user().externalId("ext-1").build()

    private fun executeWith(vararg values: Pair<String, String>): MedicalRecord {
        val builder = steekkaart()
        values.forEach { (key, value) -> builder.waarde(key, value) }
        given(ledenApi.getLidSteekkaart("ext-1", null)).willReturn(builder.build())
        testee.execute(user)
        val captor = argumentCaptor<MedicalRecord>()
        then(medicalRecordRepository).should().save(captureValue(captor))
        return captor.value
    }

    @Test
    fun `should translate the external medical record fields`() {
        // given
        val values = arrayOf(
            PHOTOGRAPHED to "ja",
            PAINKILLERS to "ja",
            FOOD to "vegetarisch",
            ALLERGIES to "noten",
            FAMILY to "gescheiden",
            SOCIAL to "verlegen",
            MEDICATIONS to "Rilatine",
            PHYSICIAN_CONTACT to "dr. Who",
        )

        // when
        val record = executeWith(*values)

        // then
        assertThat(record.user).isSameAs(user)
        assertThat(record).extracting("mayBePhotographed", "mayTakePainkillers", "foodAnomalies", "allergies",
            "familyRemarks", "socialRemarks", "medications", "physicianContact", "physician")
            .containsExactly(true, true, "vegetarisch", "noten", "gescheiden", "verlegen", "Rilatine", "dr. Who", null)
    }

    @Test
    fun `should not allow photos nor painkillers when the answer is not ja`() {
        // given

        // when
        val record = executeWith(PHOTOGRAPHED to "nee", PAINKILLERS to "Ja ")

        // then
        assertThat(record.mayBePhotographed).isFalse()
        assertThat(record.mayTakePainkillers).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "  ", "/", "nee", "NEE", "neen", "nvt", "Geen"])
    fun `should treat empty or negative answers as no remark`(answer: String) {
        // given

        // when
        val record = executeWith(ALLERGIES to answer)

        // then
        assertThat(record.allergies).isNull()
        assertThat(record.needsConfirmation).isFalse()
    }

    @Test
    fun `should join all activity restriction answers when several are filled in`() {
        // given

        // when
        val record = executeWith(IMPOSSIBLE_ACTIVITIES to "zwemmen", AT_SPORTS to "/", AT_SOCIAL to "sociaal", OTHER to "ander")

        // then
        assertThat(record.activityRestrictions).isEqualTo("zwemmen,sociaal,ander")
    }

    @Test
    fun `should leave activity restrictions empty when no restriction answer is filled in`() {
        // given

        // when
        val record = executeWith(IMPOSSIBLE_ACTIVITIES to "nee")

        // then
        assertThat(record.activityRestrictions).isNull()
    }

    @Test
    fun `should leave diseases empty when no disease answer is filled in`() {
        // given

        // when
        val record = executeWith(DISEASES to "geen")

        // then
        assertThat(record.diseases).isNull()
    }

    @Test
    fun `should not copy hygiene remarks into the diseases`() {
        // given

        // when
        val record = executeWith(DISEASES to "astma", AT_HYGIENE to "bedplassen")

        // then
        assertThat(record.diseases).isEqualTo("astma")
    }

    @ParameterizedTest
    @CsvSource("O+,OP", "O-,ON", "A+,AP", "A-,AN", "B-,BN", "AB+,ABP", "AB-,ABN", "onbekend,UNKNOWN")
    fun `should translate the blood group`(external: String, expected: BloodGroup) {
        // given

        // when
        val record = executeWith(BLOOD_GROUP to external)

        // then
        assertThat(record.bloodGroup).isEqualTo(expected)
    }

    @Test
    fun `should have an unknown blood group when it is not filled in`() {
        // given

        // when
        val record = executeWith()

        // then
        assertThat(record.bloodGroup).isEqualTo(BloodGroup.UNKNOWN)
    }

    @Test
    fun `should translate blood group B+`() {
        // given

        // when
        val record = executeWith(BLOOD_GROUP to "B+")

        // then
        assertThat(record.bloodGroup).isEqualTo(BloodGroup.BP)
    }

    companion object {
        private const val PHOTOGRAPHED = "d5f75e1e463384de014639190ebb00eb"
        private const val PAINKILLERS = "d5f75e1e463384de0146390e395900e2"
        private const val FOOD = "d5f75e1e463384de0146391a3b4800ed"
        private const val ALLERGIES = "d5f75e1e463384de0146391124af00e5"
        private const val IMPOSSIBLE_ACTIVITIES = "d5f75e1e4610ed0201461f119f740016"
        private const val AT_SPORTS = "d5f75e1e480b9aa901480c7fb70100de"
        private const val AT_HYGIENE = "d5f75e1e4610ed0201461f1464ef001a"
        private const val AT_SOCIAL = "d5f75e1e4610ed0201461f14d9de001b"
        private const val OTHER = "d5f75e1e4610ed0201461f1523c6001c"
        private const val FAMILY = "d5f75e1e4610ed0201461f026f8e0013"
        private const val SOCIAL = "d5f75e1e463384de0146391abdd000ee"
        private const val DISEASES = "d5f75e1e463384de01463905280100de"
        private const val MEDICATIONS = "d5f75e1e463384de01463901e13c00dc"
        private const val PHYSICIAN_CONTACT = "d5f75e1e463384de0146391800f100e9"
        private const val BLOOD_GROUP = "d5f75e1e463384de01463916d21b00e8"
    }
}
