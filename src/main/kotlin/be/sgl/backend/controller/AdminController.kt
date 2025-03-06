package be.sgl.backend.controller

import be.sgl.backend.service.MailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin")
class AdminController {

    @Autowired
    private lateinit var mailService: MailService

    @GetMapping("/mail")
    fun sendMail(): ResponseEntity<String> {
        mailService.builder()
            .subject("test")
            .body("teeeest")
            .to("robinkep@gmail.com")
            .send()
        return ResponseEntity.ok("Mail sent.")
    }
}
