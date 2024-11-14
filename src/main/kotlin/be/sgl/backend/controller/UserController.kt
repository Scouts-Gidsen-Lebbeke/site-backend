package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.dto.UserDTO
import be.sgl.backend.service.user.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/user")
class UserController {

    @Autowired
    lateinit var userService: UserService

    @GetMapping
    fun getProfile(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<UserDTO> {
        return ResponseEntity.ok(userService.getProfile(userDetails.username))
    }
}