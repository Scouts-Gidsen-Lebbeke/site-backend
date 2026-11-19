package be.sgl.backend.entity.user

import be.sgl.backend.entity.user.MedicalRecordTestMother.medicalRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime

class MedicalRecordTest {

    @Nested
    inner class IsUpToDate {

        @Test
        fun `should be up to date when modified less than a year ago`() {
            // given
            val testee = medicalRecord().lastModifiedDate(LocalDateTime.now().minusYears(1).plusDays(1)).build()

            // when
            val upToDate = testee.isUpToDate

            // then
            assertThat(upToDate).isTrue()
        }

        @Test
        fun `should not be up to date when modified exactly a year ago`() {
            // given
            val testee = medicalRecord().lastModifiedDate(LocalDate.now().minusYears(1).atStartOfDay()).build()

            // when
            val upToDate = testee.isUpToDate

            // then
            assertThat(upToDate).isFalse()
        }

        @Test
        fun `should not be up to date when modified more than a year ago`() {
            // given
            val testee = medicalRecord().lastModifiedDate(LocalDateTime.now().minusYears(2)).build()

            // when
            val upToDate = testee.isUpToDate

            // then
            assertThat(upToDate).isFalse()
        }

        @Test
        fun `should not be up to date when never modified`() {
            // given
            val testee = medicalRecord().lastModifiedDate(null).build()

            // when
            val upToDate = testee.isUpToDate

            // then
            assertThat(upToDate).isFalse()
        }
    }

    @Nested
    inner class NeedsConfirmation {

        @Test
        fun `should not need confirmation when no remarks are filled in`() {
            // given
            val testee = medicalRecord().physician("dr. Who").physicianContact("0470").mayBePhotographed().build()

            // when
            val needsConfirmation = testee.needsConfirmation

            // then
            assertThat(needsConfirmation).isFalse()
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("be.sgl.backend.entity.user.MedicalRecordTest#recordsWithRemarks")
        fun `should need confirmation when a remark is filled in`(field: String, testee: MedicalRecord) {
            // given

            // when
            val needsConfirmation = testee.needsConfirmation

            // then
            assertThat(needsConfirmation).isTrue()
        }
    }

    @Nested
    inner class NeedsCertificate {

        @Test
        fun `should need a certificate when medications are filled in`() {
            // given
            val testee = medicalRecord().medications("Rilatine").build()

            // when
            val needsCertificate = testee.needsCertificate

            // then
            assertThat(needsCertificate).isTrue()
        }

        @Test
        fun `should not need a certificate when only other remarks are filled in`() {
            // given
            val testee = medicalRecord().allergies("nuts").diseases("asthma").build()

            // when
            val needsCertificate = testee.needsCertificate

            // then
            assertThat(needsCertificate).isFalse()
        }
    }

    companion object {
        @JvmStatic
        fun recordsWithRemarks() = listOf(
            arrayOf("foodAnomalies", medicalRecord().foodAnomalies("vegetarian").build()),
            arrayOf("allergies", medicalRecord().allergies("nuts").build()),
            arrayOf("activityRestrictions", medicalRecord().activityRestrictions("no swimming").build()),
            arrayOf("familyRemarks", medicalRecord().familyRemarks("divorced").build()),
            arrayOf("socialRemarks", medicalRecord().socialRemarks("shy").build()),
            arrayOf("diseases", medicalRecord().diseases("asthma").build()),
            arrayOf("medications", medicalRecord().medications("Rilatine").build()),
        )
    }
}
