package be.sgl.backend.controller

import be.sgl.backend.config.BadRequestResponse
import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.service.belcotax.BelcotaxService
import be.sgl.backend.util.zipped
import generated.Verzendingen
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors

@RestController
@RequestMapping("/belcotax/{fiscalYear}")
@Tag(name = "Belcotax", description = "Endpoints for creating Belcotax forms and dispatches.")
class BelcotaxController {

    @Autowired
    private lateinit var belcotaxService: BelcotaxService

    @GetMapping("/dispatch", produces = ["application/xml"])
    @OnlyAdmin
    @Operation(
        summary = "Retrieve the Belcotax dispatch xml file.",
        description = "Generate the tax forms for the fiscal year for the current user. If multiple forms are applicable, a zip file is returned.",
        responses = [
            ApiResponse(responseCode = "200", description = "Dispatch file generated", content = [Content(mediaType = "application/xml", schema = Schema(implementation = Verzendingen::class))]),
            ApiResponse(responseCode = "400", description = "No relevant activities found", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "409", description = "Missing configuration", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun getDispatchForFiscalYear(@PathVariable fiscalYear: Int): ResponseEntity<Verzendingen> {
        return ResponseEntity.ok(belcotaxService.getDispatchForFiscalYearAndRate(fiscalYear))
    }

    @GetMapping("/form")
    @OnlyAuthenticated
    @Operation(
        summary = "Retrieve the Belcotax forms for the current user.",
        description = "Generate the tax forms for the fiscal year for the current user. If multiple forms are applicable, a zip file is returned.",
        responses = [
            ApiResponse(responseCode = "200", description = "Form(s) generated", content = [Content(mediaType = "application/octet-stream")]),
            ApiResponse(responseCode = "400", description = "No relevant activities found", content = [Content(mediaType = "application/json", schema = Schema(implementation = BadRequestResponse::class))]),
            ApiResponse(responseCode = "401", description = "User is not logged in", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "409", description = "Missing configuration", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun getUserFormsForFiscalYear(@PathVariable fiscalYear: Int, @AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ByteArray> {
        val forms = belcotaxService.getFormsForUserFiscalYearAndRate(userDetails.username, fiscalYear)
        return if (forms.size == 1) {
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(forms.first())
        } else {
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forms.zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(forms.zipped())
        }
    }

    @GetMapping("/mail")
    @OnlyAdmin
    @Operation(
        summary = "Mail the Belcotax forms to all relevant users.",
        description = "Generate the tax forms for the fiscal year for all relevant users and mail it to them. Returns a feedback stream with emits for each successful email.",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = "text/event-stream", schema = Schema(type = "string", format = "binary"))]),
            ApiResponse(responseCode = "401", description = "User has no admin role", content = [Content(schema = Schema(hidden = true))]),
            ApiResponse(responseCode = "409", description = "Missing configuration", content = [Content(schema = Schema(hidden = true))])
        ]
    )
    fun mailFormsForFiscalYear(@PathVariable fiscalYear: Int): SseEmitter {
        val emitter = SseEmitter()
        Executors.newSingleThreadExecutor().submit {
            emitter.send("Generating forms...")
            val forms = belcotaxService.getFormsForFiscalYearAndRate(fiscalYear)
            try {
                forms.onEachIndexed { i, (user, userForms) ->
                    emitter.send("Sending email $i of ${forms.size}")
                    belcotaxService.mailFormsToUser(fiscalYear, user, userForms)
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