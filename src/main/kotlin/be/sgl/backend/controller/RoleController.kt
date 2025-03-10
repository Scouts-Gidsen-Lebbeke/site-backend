package be.sgl.backend.controller

import be.sgl.backend.service.RoleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/roles")
class RoleController {

    @Autowired
    private lateinit var roleService: RoleService
}
