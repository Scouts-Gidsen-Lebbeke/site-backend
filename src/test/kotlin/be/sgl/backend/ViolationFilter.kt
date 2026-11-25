package be.sgl.backend

object ViolationFilter {

    private val SOURCE_MODULE = Regex("^Module '([^']+)'")
    private val CYCLE_SLICE = Regex("""Slice (\w+)""")

    fun isOffending(message: String, strictModules: Collection<String>): Boolean =
        if (message.startsWith("Cycle detected")) {
            CYCLE_SLICE.findAll(message).any { it.groupValues[1] in strictModules }
        } else {
            SOURCE_MODULE.find(message)?.groupValues?.get(1) in strictModules
        }
}
