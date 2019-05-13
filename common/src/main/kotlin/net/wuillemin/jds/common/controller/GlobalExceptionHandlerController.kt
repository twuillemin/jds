package net.wuillemin.jds.common.controller

import net.wuillemin.jds.common.dto.RestMessage
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.exception.AuthenticationRejectedException
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.exception.ClientException
import net.wuillemin.jds.common.exception.ConstraintException
import net.wuillemin.jds.common.exception.CriticalConstraintException
import net.wuillemin.jds.common.exception.DeniedPermissionException
import net.wuillemin.jds.common.exception.NotFoundException
import net.wuillemin.jds.common.service.LocalisationService
import net.wuillemin.jds.common.service.UserService
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.*


/**
 * A specific controller to handle exception that can be thrown by the application
 *
 * @param localisationService The service for accessing localization
 * @param userService The service for getting [User] information
 * @param logger The logger
 */
@RestControllerAdvice
class GlobalExceptionHandlerController(
    private val localisationService: LocalisationService,
    private val userService: UserService,
    private val logger: Logger) {

    // ============================================================
    // Standard Java Exceptions
    // ============================================================

    /**
     * Catch IllegalArgumentException
     *
     * @param exception The exception
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [IllegalArgumentException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun illegalArgumentExceptionHandler(exception: IllegalArgumentException): String {

        return "Bad request (${exception.message})"
    }

    // ============================================================
    // Authentication exceptions
    // ============================================================

    /**
     * Catch DisabledException
     *
     * @param exception The exception
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [DisabledException::class])
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun disabledExceptionHandler(exception: DisabledException): String {
        return "Account is disabled"
    }

    /**
     * Catch LockedException
     *
     * @param exception The exception
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [LockedException::class])
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun lockedExceptionHandler(exception: LockedException): String {
        return "Account is locked"
    }

    /**
     * Catch BadCredentialsException
     *
     * @param exception The exception
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [BadCredentialsException::class])
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun badCredentialsExceptionHandler(exception: BadCredentialsException): String {
        return "Incorrect credentials"
    }

    // ============================================================
    // Custom Exception
    // ============================================================

    /**
     * Catch BadParameterException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [BadParameterException::class])
    fun badParameterExceptionHandler(
        exception: BadParameterException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)
        val origin = getOrigin(exception)

        // Give an info, nothing critical here
        logger.info("$origin: $message")

        return ResponseEntity(
            RestMessage(message),
            if (origin.contains("Controller")) {
                HttpStatus.BAD_REQUEST
            }
            else {
                HttpStatus.INTERNAL_SERVER_ERROR
            })
    }

    /**
     * Catch ClientException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [ClientException::class])
    fun clientExceptionHandler(
        exception: ClientException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)


        // Give a Warning as we could have loose a connector
        logger.warn("${getOrigin(exception)}: $message")

        return ResponseEntity(
            RestMessage(message),
            HttpStatus.BAD_GATEWAY)
    }

    /**
     * Catch ConstraintException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [ConstraintException::class])
    fun constraintExceptionHandler(
        exception: ConstraintException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)

        // Give an info, nothing critical here
        logger.info("${getOrigin(exception)}: $message")

        return ResponseEntity(
            RestMessage(message),
            HttpStatus.CONFLICT)
    }

    /**
     * Catch CriticalConstraintException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [CriticalConstraintException::class])
    fun criticalConstraintExceptionHandler(
        exception: CriticalConstraintException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)

        // Raise an Error as it is critical
        logger.error("${getOrigin(exception)}: $message")

        return ResponseEntity(
            RestMessage(message),
            HttpStatus.CONFLICT)
    }

    /**
     * Catch DeniedPermissionException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [DeniedPermissionException::class])
    fun deniedPermissionExceptionHandler(
        exception: DeniedPermissionException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)
        val user = try {
            userService.getUserById(exception.userId)
        }
        catch (e: Exception) {
            logger.error("deniedPermissionExceptionHandler: a DeniedPermissionException was received with a non valid userId: '${exception.userId}'")
            User(null, "ERROR", "", "", "", true, Profile.USER, emptySet())
        }

        // Give a warning because it may be a bad actor
        logger.warn("${getOrigin(exception)}: ${user.getLoggingId()} tried to commit a crime: $message")

        return ResponseEntity(
            RestMessage(localisationService.getMessage(exception.code, exception.args, locale)),
            HttpStatus.FORBIDDEN)
    }

    /**
     * Catch AuthenticationRejectedException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [AuthenticationRejectedException::class])
    fun authenticationRejectedExceptionHandler(
        exception: AuthenticationRejectedException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)

        // Give a warning because it may be a bad actor
        logger.warn("${getOrigin(exception)}: $message")

        return ResponseEntity(
            RestMessage(message),
            HttpStatus.FORBIDDEN)
    }

    /**
     * Catch NotFoundException
     *
     * @param exception The exception
     * @param locale The locale
     * @return the error message to be returned to the user
     */
    @ExceptionHandler(value = [NotFoundException::class])
    fun notFoundExceptionHandler(
        exception: NotFoundException,
        locale: Locale): ResponseEntity<RestMessage> {

        val message = localisationService.getMessage(exception.code, exception.args, locale)

        // Give a warning because it may be a bad actor
        logger.info("${getOrigin(exception)}: $message")

        return ResponseEntity(
            RestMessage(message),
            HttpStatus.NOT_FOUND)
    }

    /**
     * Get the origin of an exception
     */
    private fun getOrigin(exception: Exception): String {
        return "${exception::class.simpleName} at ${exception.stackTrace[0].className}::${exception.stackTrace[0].methodName} (line ${exception.stackTrace[0].lineNumber})"
    }
}
