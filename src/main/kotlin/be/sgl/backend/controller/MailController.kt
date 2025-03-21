package be.sgl.backend.controller

import be.sgl.backend.config.security.OnlyStaff
import be.sgl.backend.dto.MailDTO
import be.sgl.backend.service.MailService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors

@Controller
@RequestMapping("/mails")
@Tag(name = "Mail", description = "Endpoints for managing mails.")
class MailController {

    @Autowired
    private lateinit var mailService: MailService

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @OnlyStaff
    @Operation(
        summary = "Send mails to recipients.",
        description = "Sends mails based on the provided data. Returns a feedback stream with emits for each successful email.",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = TEXT_EVENT_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])
        ]
    )
    fun sendMails(@RequestPart("file", required = false) attachment: MultipartFile?, @Valid @ModelAttribute mailDTO: MailDTO): SseEmitter {
        val emitter = SseEmitter()
        Executors.newSingleThreadExecutor().submit {
            try {
                mailDTO.to.onEachIndexed { i, to ->
                    emitter.send("Sending email $i of ${mailDTO.to.size}")
                    val builder = mailService.builder()
                        .from(mailDTO.from)
                        .to(to)
                        .subject(mailDTO.subject)
                        .body(mailDTO.body)
                    if (i == 0) mailDTO.cc?.let { builder.cc(it) }
                    attachment?.let { builder.addAttachment(it.inputStream, it.originalFilename ?: it.name) }
                    builder.send()
                }
                emitter.send("All emails sent successfully!")
                emitter.complete()
            } catch (e: Exception) {
                emitter.send("Error occurred: ${e.message}")
                emitter.completeWithError(e)
            }
        }
        return emitter
    }
}
