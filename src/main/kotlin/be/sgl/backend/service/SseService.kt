package be.sgl.backend.service

import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Component
class SseService {

    private val emitters: MutableMap<String, SseEmitter> = ConcurrentHashMap()

    fun schedule(runnable: (SseEmitter) -> Unit): String {
        val emitter = SseEmitter()
        val emitterId = UUID.randomUUID().toString()
        emitters[emitterId] = emitter
        Executors.newSingleThreadExecutor().submit {
            try {
                runnable.invoke(emitter)
            } catch (e: Exception) {
                emitter.send("Error occurred: ${e.message}")
                emitter.completeWithError(e)
            } finally {
                emitters.remove(emitterId)
            }
        }
        return emitterId
    }

    fun getEmitter(emitterId: String): SseEmitter? {
        return emitters[emitterId]
    }
}