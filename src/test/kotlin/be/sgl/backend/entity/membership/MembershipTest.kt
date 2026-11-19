package be.sgl.backend.entity.membership

import be.sgl.backend.entity.PaymentTestMother.membership
import be.sgl.backend.entity.membership.MembershipPeriodTestMother.membershipPeriod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MembershipTest {

    @Test
    fun `should describe the membership with its period`() {
        // given
        val period = membershipPeriod().start(LocalDate.of(2025, 9, 1)).end(LocalDate.of(2026, 8, 31)).build()
        val testee = membership().period(period).build()

        // when
        val description = testee.getDescription()

        // then
        assertThat(description).isEqualTo("Lidgeld 01/09/2025 - 31/08/2026")
    }
}
