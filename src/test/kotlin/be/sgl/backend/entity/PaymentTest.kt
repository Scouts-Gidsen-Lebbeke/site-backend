package be.sgl.backend.entity

import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaymentTest {

    @Test
    fun `should be paid when marked paid`() {
        // given
        val payment = activityRegistration().paid(false).build()

        // when
        val result = payment.markPaid()

        // then
        assertThat(result).isSameAs(payment)
        assertThat(payment.paid).isTrue()
    }

    @Test
    fun `should no longer be paid when marked refunded`() {
        // given
        val payment = activityRegistration().paid().build()

        // when
        val result = payment.markRefunded()

        // then
        assertThat(result).isSameAs(payment)
        assertThat(payment.paid).isFalse()
    }
}
