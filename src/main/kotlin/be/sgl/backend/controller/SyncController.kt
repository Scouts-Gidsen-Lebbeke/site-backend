package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.Public
import be.sgl.backend.service.SseService
import be.sgl.backend.service.user.SyncService
import be.sgl.backend.util.ForExternalOrganization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Controller
@ForExternalOrganization
@RequestMapping("/sync")
class SyncController {

    @Autowired
    private lateinit var sseService: SseService
    @Autowired
    private lateinit var syncService: SyncService
    @Value("\${organization.external.id}")
    lateinit var externalOrganizationId: String

    @GetMapping("/users")
    @OnlyAdmin
    fun syncUsers(@RequestParam(required = false, defaultValue = "false") withData: Boolean): SseEmitter {
        TODO("Fetch all users based on the organization id and create the users")
        // one time thing to fill up db
        // withData param to fetch all external data and save internally, if GA ever decides to stop
    }

    @GetMapping("/members")
    @OnlyAdmin
    fun syncMembers(): String {
        return sseService.schedule(syncService::syncMembers)
    }

    @GetMapping("/external-id")
    @Public
    fun getExternalId(): ResponseEntity<String> {
        return ResponseEntity.ok(externalOrganizationId)
    }
}
