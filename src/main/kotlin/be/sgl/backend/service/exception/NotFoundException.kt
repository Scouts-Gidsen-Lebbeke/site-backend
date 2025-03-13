package be.sgl.backend.service.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
abstract class NotFoundException(message: String) : Throwable(message)

class NewsItemNotFoundException : NotFoundException("News item not found")

class OrganizationNotFoundException : NotFoundException("Organization not found")

class CalendarPeriodNotFoundException : NotFoundException("Calendar period not found")

class CalendarNotFoundException : NotFoundException("Calendar not found")

class CalendarItemNotFoundException : NotFoundException("Calendar item not found")

class BranchNotFoundException : NotFoundException("Branch not found")

class ActivityNotFoundException : NotFoundException("Activity not found")

class RestrictionNotFoundException : NotFoundException("Restriction not found")

class ActivityRegistrationNotFoundException : NotFoundException("Activity registration not found")

class EventNotFoundException : NotFoundException("Event not found")

class EventRegistrationNotFoundException : NotFoundException("Event registration not found")

class RoleNotFoundException : NotFoundException("Role not found")

class MembershipNotFoundException : NotFoundException("Membership not found")
