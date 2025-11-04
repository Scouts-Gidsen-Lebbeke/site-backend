package be.sgl.backend.service.exception

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

class NotFoundExceptionTest {

    @Test
    fun `NewsItemNotFoundException should have correct message`() {
        val exception = NewsItemNotFoundException()
        assertEquals("News item not found", exception.message)
    }

    @Test
    fun `NewsItemNotFoundException should be annotated with ResponseStatus NOT_FOUND`() {
        val annotation = NewsItemNotFoundException::class.java.superclass.getAnnotation(ResponseStatus::class.java)
        assertNotNull(annotation)
        assertEquals(HttpStatus.NOT_FOUND, annotation?.value)
    }

    @Test
    fun `OrganizationNotFoundException should have correct message`() {
        val exception = OrganizationNotFoundException()
        assertEquals("Organization not found", exception.message)
    }

    @Test
    fun `CalendarPeriodNotFoundException should have correct message`() {
        val exception = CalendarPeriodNotFoundException()
        assertEquals("Calendar period not found", exception.message)
    }

    @Test
    fun `CalendarNotFoundException should have correct message`() {
        val exception = CalendarNotFoundException()
        assertEquals("Calendar not found", exception.message)
    }

    @Test
    fun `CalendarItemNotFoundException should have correct message`() {
        val exception = CalendarItemNotFoundException()
        assertEquals("Calendar item not found", exception.message)
    }

    @Test
    fun `BranchNotFoundException should have correct message`() {
        val exception = BranchNotFoundException()
        assertEquals("Branch not found", exception.message)
    }

    @Test
    fun `ActivityNotFoundException should have correct message`() {
        val exception = ActivityNotFoundException()
        assertEquals("Activity not found", exception.message)
    }

    @Test
    fun `RestrictionNotFoundException should have correct message`() {
        val exception = RestrictionNotFoundException()
        assertEquals("Restriction not found", exception.message)
    }

    @Test
    fun `ActivityRegistrationNotFoundException should have correct message`() {
        val exception = ActivityRegistrationNotFoundException()
        assertEquals("Activity registration not found", exception.message)
    }

    @Test
    fun `EventNotFoundException should have correct message`() {
        val exception = EventNotFoundException()
        assertEquals("Event not found", exception.message)
    }

    @Test
    fun `EventRegistrationNotFoundException should have correct message`() {
        val exception = EventRegistrationNotFoundException()
        assertEquals("Event registration not found", exception.message)
    }

    @Test
    fun `RoleNotFoundException should have correct message`() {
        val exception = RoleNotFoundException()
        assertEquals("Role not found", exception.message)
    }

    @Test
    fun `MembershipNotFoundException should have correct message`() {
        val exception = MembershipNotFoundException()
        assertEquals("Membership not found", exception.message)
    }

    @Test
    fun `UserNotFoundException should have correct message with username`() {
        val exception = UserNotFoundException("john.doe")
        assertEquals("User john.doe not found", exception.message)
    }

    @Test
    fun `MembershipPeriodNotFoundException should have correct message`() {
        val exception = MembershipPeriodNotFoundException()
        assertEquals("Membership period not found", exception.message)
    }

    @Test
    fun `all NotFoundException subclasses should extend NotFoundException`() {
        assertTrue(NewsItemNotFoundException() is NotFoundException)
        assertTrue(OrganizationNotFoundException() is NotFoundException)
        assertTrue(CalendarNotFoundException() is NotFoundException)
        assertTrue(BranchNotFoundException() is NotFoundException)
        assertTrue(ActivityNotFoundException() is NotFoundException)
        assertTrue(RoleNotFoundException() is NotFoundException)
        assertTrue(MembershipNotFoundException() is NotFoundException)
        assertTrue(UserNotFoundException("test") is NotFoundException)
    }
}
