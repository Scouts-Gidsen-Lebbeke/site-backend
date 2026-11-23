package be.sgl.backend.entity.registrable

import be.sgl.backend.entity.PayableTestMother.event
import be.sgl.backend.entity.PaymentTestMother.eventRegistration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RegistrationTest {

    @Test
    fun `should use the registrable name as description`() {
        // given
        val event = event().name("Spaghettiavond").build()
        val registration = eventRegistration().event(event).build()

        // when
        val description = registration.getDescription()

        // then
        assertThat(description).isEqualTo(event.name)
    }

    @Test
    fun `should parse additional data to a map when additional data is present`() {
        // given
        val registration = eventRegistration().additionalData("""{"size":"M","count":2}""").build()

        // when
        val map = registration.getAdditionalDataMap()

        // then
        assertThat(map).isEqualTo(mapOf("size" to "M", "count" to 2))
    }

    @Test
    fun `should return null as additional data map when no additional data is present`() {
        // given
        val registration = eventRegistration().build()

        // when
        val map = registration.getAdditionalDataMap()

        // then
        assertThat(map).isNull()
    }
}
