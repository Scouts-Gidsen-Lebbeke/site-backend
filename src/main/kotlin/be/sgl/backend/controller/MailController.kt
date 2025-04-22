package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.MailDTO
import be.sgl.backend.service.MailService
import be.sgl.backend.service.SseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Controller
@RequestMapping("/mails")
@Tag(name = "Mail", description = "Endpoints for managing mails.")
class MailController {

    @Autowired
    private lateinit var mailService: MailService
    @Autowired
    private lateinit var sseService: SseService

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Send mails to recipients.",
        description = "Sends mails based on the provided data. Returns the id to a feedback stream with emits for each successful email.",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = TEXT_PLAIN_VALUE)])
        ]
    )
    fun sendMails(@RequestPart("file", required = false) attachment: MultipartFile?, @Valid @ModelAttribute mailDTO: MailDTO): String {
        val builder = mailService.builder()
            .from(mailDTO.from)
            .subject(mailDTO.subject)
            .body(mailDTO.body)
        return sseService.schedule { emitter ->
            attachment?.let { builder.addAttachment(it.inputStream, it.originalFilename ?: it.name) }
            mailDTO.to.onEachIndexed { i, to ->
                emitter.send("Sending email $i of ${mailDTO.to.size}")
                if (i == 0) mailDTO.cc?.let { builder.cc(it) }
                builder.to(to).send()
            }
            emitter.send("All emails sent successfully!")
            emitter.complete()
        }
    }
}
