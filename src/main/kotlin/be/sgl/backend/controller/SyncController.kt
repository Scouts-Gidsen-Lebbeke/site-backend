package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.util.ForExternalOrganization
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Controller
@ForExternalOrganization
@RequestMapping("/sync")
class SyncController {

    @GetMapping("/users")
    @OnlyAdmin
    fun syncUsers(@RequestParam(required = false, defaultValue = "false") withData: Boolean): SseEmitter {
        TODO("Fetch all users based on the organization id and create the users")
        // one time thing to fill up db
        // withData param to fetch all external data and save internally, if GA ever decides to stop
    }

    @GetMapping("/members")
    @OnlyAdmin
    fun syncMembers(): SseEmitter {
        TODO("Match all current users with memberships with the external user data provider and their roles")
        // fetch all external users for this organization
        // for all users with an active membership:
        //  => check if external subscription is accepted (do so if not)
        //  => check if, when no username is set, if the external member id is available (send mail about account creation if so)
        //  => check if member roles are applied externally (lookup role based on branch and apply (backup)ExternalIds)
        //  => remove from external user list after all checks
        // for remaining users in external user list:
        //  => remove all roles that are known here (via (backup)ExternalIds) and send mail of ending membership
    }

    @GetMapping("/staff")
    @OnlyAdmin
    fun syncStaffData(): SseEmitter {
        TODO("Fetch and update all staff data")
        // should not be relevant anymore since staff role assignment is propagated directly? and staff data is internal
    }
}
