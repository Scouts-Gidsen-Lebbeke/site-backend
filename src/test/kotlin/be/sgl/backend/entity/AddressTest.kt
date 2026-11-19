package be.sgl.backend.entity

import be.sgl.backend.entity.AddressTestMother.address
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AddressTest {

    @Test
    fun `should combine street and number when there is no sub premise`() {
        // given
        val testee = address().street("Kerkstraat").number("12").build()

        // when
        val streetAddress = testee.getStreetAdress()

        // then
        assertThat(streetAddress).isEqualTo("Kerkstraat 12")
    }

    @Test
    fun `should append the sub premise directly after the number when there is one`() {
        // given
        val testee = address().street("Kerkstraat").number("12").subPremise("B").build()

        // when
        val streetAddress = testee.getStreetAdress()

        // then
        assertThat(streetAddress).isEqualTo("Kerkstraat 12B")
    }

    @Test
    fun `should describe the full address including country when converted to string`() {
        // given
        val testee = address().street("Kerkstraat").number("12").zipcode("9280").town("Lebbeke").country("BE").build()

        // when
        val description = testee.toString()

        // then
        assertThat(description).isEqualTo("Kerkstraat 12, 9280 Lebbeke (BE)")
    }
}
