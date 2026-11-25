package be.sgl.backend.access

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@PackageInfo
@ApplicationModule(allowedDependencies = ["shared", "organization", "people", "setting"])
class AccessModule
