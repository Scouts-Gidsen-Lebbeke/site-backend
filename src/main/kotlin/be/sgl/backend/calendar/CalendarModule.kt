package be.sgl.backend.calendar

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@PackageInfo
@ApplicationModule(allowedDependencies = ["shared", "organization"])
class CalendarModule
