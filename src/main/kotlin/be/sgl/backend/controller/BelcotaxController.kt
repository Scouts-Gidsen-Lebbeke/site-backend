package be.sgl.backend.controller

import be.sgl.backend.config.CustomUserDetails
import be.sgl.backend.config.security.OnlyAdmin
import be.sgl.backend.config.security.OnlyAuthenticated
import be.sgl.backend.service.SseService
import be.sgl.backend.service.belcotax.BelcotaxService
import be.sgl.backend.util.zipped
import generated.Verzendingen
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/belcotax")
@Tag(name = "Belcotax", description = "Endpoints for creating Belcotax forms and dispatches.")
class BelcotaxController {

    @Autowired
    private lateinit var belcotaxService: BelcotaxService
    @Autowired
    private lateinit var sseService: SseService

    @GetMapping("/dispatch")
    @OnlyAdmin
    @Operation(
        summary = "Retrieve the Belcotax dispatch xml file.",
        description = "Generate the tax forms for the passed fiscal year for the current user. If multiple forms are applicable, a zip file is returned.",
        responses = [
            ApiResponse(responseCode = "200", description = "Dispatch file generated", content = [Content(mediaType = APPLICATION_XML_VALUE, schema = Schema(implementation = Verzendingen::class))]),
            ApiResponse(responseCode = "400", description = "No relevant activities found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Missing configuration", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getDispatchForPreviousYear(): ResponseEntity<Verzendingen> {
        return ResponseEntity.ok(belcotaxService.getDispatchForPreviousYear())
    }

    @GetMapping("/form")
    @OnlyAuthenticated
    @Operation(
        summary = "Retrieve the Belcotax forms for the current user.",
        description = "Generate the tax forms for the previous fiscal year for the current user. If multiple forms are applicable, a zip file is returned.",
        responses = [
            ApiResponse(responseCode = "200", description = "Form(s) generated", content = [Content(mediaType = APPLICATION_OCTET_STREAM_VALUE)]),
            ApiResponse(responseCode = "400", description = "No relevant activities found", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Missing configuration", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun getFormsForUserAndPreviousYear(@AuthenticationPrincipal userDetails: CustomUserDetails): ResponseEntity<ByteArray> {
        val forms = belcotaxService.getFormsForUserAndPreviousYear(userDetails.username)
        return if (forms.size == 1) {
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"form.pdf\"")
                .contentType(APPLICATION_PDF)
                .body(forms.first())
        } else {
            ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"forms.zip\"")
                .contentType(APPLICATION_OCTET_STREAM)
                .body(forms.zipped())
        }
    }

    @GetMapping("/mail")
    @OnlyAdmin
    @Operation(
        summary = "Mail the Belcotax forms to all relevant users.",
        description = "Generate the tax forms for the previous fiscal year for all relevant users and mail it to them. Returns a feedback stream with emits for each successful email.",
        responses = [
            ApiResponse(responseCode = "200", description = "SSE stream established", content = [Content(mediaType = TEXT_PLAIN_VALUE)]),
            ApiResponse(responseCode = "409", description = "Missing configuration", content = [Content(mediaType = APPLICATION_JSON_VALUE, schema = Schema(implementation = ApiErrorResponse::class))])
        ]
    )
    fun mailFormsForPreviousYear(): String {
        val forms = belcotaxService.getFormsForPreviousYear()
        return sseService.schedule { emitter ->
            forms.onEachIndexed { i, (user, userForms) ->
                emitter.send("Sending email $i of ${forms.size}")
                belcotaxService.mailFormsToUser(user, userForms)
            }
            emitter.send("All emails sent successfully!")
            emitter.complete()
        }
    }
}