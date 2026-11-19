package be.sgl.backend.service.belcotax

import be.sgl.backend.entity.AddressTestMother.address
import be.sgl.backend.entity.PaymentTestMother.activityRegistration
import be.sgl.backend.entity.user.ContactTestMother.contact
import be.sgl.backend.entity.user.UserTestMother.user
import be.sgl.backend.repository.user.ContactRepository
import be.sgl.backend.service.SettingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FilterIntoValidFormDataTest {

    @Mock
    lateinit var settingService: SettingService
    @Mock
    lateinit var contactRepository: ContactRepository
    @InjectMocks
    lateinit var testee: FilterIntoValidFormData

    private val year = 2025
    private val defaultRate = 14.4
    private val postalAddress = address().street("Postweg").postalAddress().build()

    private fun registrations(count: Int) = (1..count).map {
        activityRegistration().id(it).start(LocalDateTime.of(year, it, 1, 10, 0)).end(LocalDateTime.of(year, it, 3, 10, 0)).build()
    }

    @Nested
    inner class WhenUserDataIsIncomplete {

        @Test
        fun `should return no forms when user has no nis`() {
            // given
            val user = user().addresses(postalAddress).build()

            // when
            val forms = testee.execute(user, registrations(1))

            // then
            assertThat(forms).isEmpty()
            then(contactRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should return no forms when user has no postal address`() {
            // given
            val user = user().nis("15031512383").addresses(address().build()).build()

            // when
            val forms = testee.execute(user, registrations(1))

            // then
            assertThat(forms).isEmpty()
            then(contactRepository).shouldHaveNoInteractions()
        }

        @Test
        fun `should return no forms when user has no parent with nis`() {
            // given
            val user = user().nis("15031512383").addresses(postalAddress).build()
            given(contactRepository.findContactsByUserAndNisNotNull(user)).willReturn(emptyList())

            // when
            val forms = testee.execute(user, registrations(1))

            // then
            assertThat(forms).isEmpty()
            then(settingService).shouldHaveNoInteractions()
        }
    }

    @Nested
    inner class WhenUserDataIsComplete {

        private val user = user().nis("15031512383").addresses(address().build(), postalAddress).build()
        private val parent = contact().user(user).build()

        @Test
        fun `should create one form linked to the postal address and first parent when there is one registration`() {
            // given
            val otherParent = contact().id(2).user(user).build()
            val registrations = registrations(1)
            given(contactRepository.findContactsByUserAndNisNotNull(user)).willReturn(listOf(parent, otherParent))
            given(settingService.getOrDefault("DISPATCH_RATE_$year", defaultRate)).willReturn(defaultRate)

            // when
            val forms = testee.execute(user, registrations)

            // then
            assertThat(forms).singleElement().satisfies({ form ->
                assertThat(form.user).isEqualTo(user)
                assertThat(form.address).isEqualTo(postalAddress)
                assertThat(form.parent).isEqualTo(parent)
                assertThat(form.activity1).isEqualTo(registrations[0])
                assertThat(form.activity2).isNull()
                assertThat(form.activity3).isNull()
                assertThat(form.activity4).isNull()
                assertThat(form.rate).isEqualTo(defaultRate)
            })
        }

        @Test
        fun `should group four registrations on a single form when there are exactly four registrations`() {
            // given
            val registrations = registrations(4)
            given(contactRepository.findContactsByUserAndNisNotNull(user)).willReturn(listOf(parent))
            given(settingService.getOrDefault("DISPATCH_RATE_$year", defaultRate)).willReturn(defaultRate)

            // when
            val forms = testee.execute(user, registrations)

            // then
            assertThat(forms).singleElement().satisfies({ form ->
                assertThat(listOf(form.activity1, form.activity2, form.activity3, form.activity4)).isEqualTo(registrations)
            })
        }

        @Test
        fun `should start a new form with a new sequence number when there are more than four registrations`() {
            // given
            val registrations = registrations(5)
            given(contactRepository.findContactsByUserAndNisNotNull(user)).willReturn(listOf(parent))
            given(settingService.getOrDefault("DISPATCH_RATE_$year", defaultRate)).willReturn(defaultRate)

            // when
            val forms = testee.execute(user, registrations)

            // then
            assertThat(forms).hasSize(2)
            assertThat(forms[1].activity1).isEqualTo(registrations[4])
            assertThat(forms[1].activity2).isNull()
            assertThat(forms.map { it.id.substringAfterLast("-") }).containsExactly("1", "2")
        }

        @Test
        fun `should use the configured dispatch rate of the activity year when one is configured`() {
            // given
            val configuredRate = 16.1
            given(contactRepository.findContactsByUserAndNisNotNull(user)).willReturn(listOf(parent))
            given(settingService.getOrDefault("DISPATCH_RATE_$year", defaultRate)).willReturn(configuredRate)

            // when
            val forms = testee.execute(user, registrations(1))

            // then
            assertThat(forms).singleElement().extracting { it.rate }.isEqualTo(configuredRate)
        }

        @Test
        @Disabled("Possible bug: registrations.first() throws NoSuchElementException for an empty list instead of returning no forms")
        fun `should return no forms when there are no registrations`() {
            // given
            given(contactRepository.findContactsByUserAndNisNotNull(user)).willReturn(listOf(parent))

            // when
            val forms = testee.execute(user, emptyList())

            // then
            assertThat(forms).isEmpty()
        }
    }
}
