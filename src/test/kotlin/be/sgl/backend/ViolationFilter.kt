package be.sgl.backend

object ViolationFilter {

    private val SOURCE_MODULE = Regex("^Module '([^']+)'")
    private val FIELD_INJECTION = Regex("""^Module \w+ uses field injection in be\.sgl\.backend\.(\w+)\.""")
    private val CYCLE_SLICE = Regex("""Slice (\w+)""")

    fun isOffending(message: String, strictModules: Collection<String>): Boolean =
        if (message.startsWith("Cycle detected")) {
            CYCLE_SLICE.findAll(message).any { it.groupValues[1] in strictModules }
        } else {
            val moduleName = SOURCE_MODULE.find(message) ?: FIELD_INJECTION.find(message)
            moduleName?.let { it.groupValues[1] in strictModules } ?: true
        }
}
