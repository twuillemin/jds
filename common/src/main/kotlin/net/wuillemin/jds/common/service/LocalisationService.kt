package net.wuillemin.jds.common.service

import net.wuillemin.jds.common.entity.Loggable
import net.wuillemin.jds.common.exception.E
import net.wuillemin.jds.common.exception.ExceptionCode
import org.springframework.context.MessageSource
import org.springframework.context.NoSuchMessageException
import org.springframework.stereotype.Service
import java.util.*
import kotlin.reflect.KClass

/**
 * Service for accessing localized messages
 *
 * @param messageSource The Spring MessageSource object
 */
@Service
class LocalisationService(private val messageSource: MessageSource) {

    /**
     * Retrieve a message.
     *
     * @param exceptionCode The code of the message that will be interpreted as a String
     * @param args The arguments associated with the code
     * @param locale The locale
     * @return a nicely formatted error message
     */
    fun getMessage(exceptionCode: ExceptionCode, args: Array<out Any?>, locale: Locale): String {

        val cleanArgs = cleanArguments(args)

        return try {
            messageSource.getMessage(exceptionCode.code, cleanArgs, locale)
        }
        catch (unknownException: NoSuchMessageException) {
            messageSource.getMessage(E.service.localization.unknownCode.code, arrayOf(exceptionCode.code, args), locale)
        }
        catch (exception: Exception) {
            // If it is not possible to retrieve messages, hard coded english message
            "An exception was raised while retrieving the message for the error code ${exceptionCode.code}"
        }
    }


    /**
     * Clean a list of parameters to ensure a smooth output
     *
     * @param args an array of arguments
     * @return a cleaned array of arguments
     */
    fun cleanArguments(args: Array<out Any?>): Array<Any> {

        return args
            .map { arg ->
                when (arg) {
                    is Loggable      -> arg.getLoggingId()
                    is KClass<*>     -> arg.simpleName
                    is Class<*>      -> arg.simpleName
                    is Collection<*> -> arg.joinToString(separator = ", ", prefix = "[", postfix = "]")
                    else             -> arg
                }
            }
            .map { arg -> arg ?: "(null)" }
            .toTypedArray()
    }
}