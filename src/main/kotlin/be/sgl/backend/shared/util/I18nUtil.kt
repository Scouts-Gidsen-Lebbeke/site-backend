package be.sgl.backend.shared.util

import be.sgl.backend.shared.config.LocaleConfig.Companion.BE_NL
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class I18nUtil : ApplicationContextAware {

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        messageSource = applicationContext.getBean(MessageSource::class.java)
    }

    companion object {

        private lateinit var messageSource: MessageSource

        @Suppress("UNCHECKED_CAST")
        fun i18n(key: String, vararg args: Any?): String {
            return messageSource.getMessage(key, args as Array<Any>, BE_NL)
        }
    }
}
