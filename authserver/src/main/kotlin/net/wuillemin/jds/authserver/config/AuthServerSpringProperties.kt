package net.wuillemin.jds.authserver.config

import net.wuillemin.jds.common.security.utils.CertificateFileReader
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.nio.file.Paths


/**
 * Main class for the configuration of the Authentication
 */
@Configuration
@ConfigurationProperties(AuthServerSpringProperties.CONTEXT)
class AuthServerSpringProperties(private val certificateFileReader: CertificateFileReader) {

    // Declare class constants
    companion object {
        /**
         * Define the context of the settings for authentication
         */
        const val CONTEXT: String = "jds.authserver"
    }

    /**
     * Construct a bean having all the properties verified and ready to be used
     *
     * @return an AuthServerProperties bean with all properties validated
     */
    @Bean
    fun buildAuthServerProperties(): AuthServerProperties {

        // Valid the base attributes
        val validPrivateKey = privateKeyPath.let {
            if (it.isBlank()) {
                throw BeanCreationException("The property privateKey must not be blank")
            }
            try {
                certificateFileReader.readDERPrivateKey(Paths.get(File(it).absolutePath))
            }
            catch (e: Exception) {
                throw BeanCreationException("Unable to read the private key", e)
            }
        }

        val validTokenTimeToLiveInSeconds = tokenTimeToLiveInSeconds.let {
            if (it < 10) {
                throw BeanCreationException("The property tokenTimeToLiveInSeconds must not be lower than 10 seconds")
            }
            it
        }

        val validRefreshTimeToLiveInSeconds = refreshTimeToLiveInSeconds.let {
            if (it < 10) {
                throw BeanCreationException("The property refreshTimeToLiveInSeconds must not be lower than 10 seconds")
            }
            it
        }

        // Get the local users if any
        val validLocalUsers = localUsers
            ?.map {
                AuthServerProperties.LocalUserProperties(
                    getAttributeLong(it.userId, "userId"),
                    getAttribute(it.userName, "userName"),
                    getAttribute(it.password, "password"),
                    getAttribute(it.profile, "profile"))
            }
            ?: emptyList()

        return AuthServerProperties(
            validPrivateKey,
            validTokenTimeToLiveInSeconds,
            validRefreshTimeToLiveInSeconds,
            validLocalUsers)
    }

    /**
     * A simple function validating that the attribute is present and not blank and converting returning it
     */
    fun getAttributeLong(attribute: Long?, attributeName: String): Long {
        return attribute
            ?: throw BeanCreationException("One of the locally defined user has its attribute $attributeName not defined")
    }

    /**
     * A simple function validating that the attribute is present and not blank and converting returning it
     */
    fun getAttribute(attribute: String?, attributeName: String): String {
        return attribute
            ?.let {
                if (it.isBlank()) {
                    throw BeanCreationException("One of the locally defined user has its attribute $attributeName defined as blank")
                }
                it
            }
            ?: throw BeanCreationException("One of the locally defined user has its attribute $attributeName not defined")
    }

    /**
     * The path of to the file holding the private key
     */
    var privateKeyPath: String = ""

    /**
     * The time to live of an authentication token
     */
    var tokenTimeToLiveInSeconds: Long = 120L

    /**
     * The time to live of a refresh token
     */
    var refreshTimeToLiveInSeconds: Long = 12000L

    /**
     * The list of locally defined users (if any)
     */
    var localUsers: List<LocalUserSpringProperties>? = null

    /**
     * The definition of locally given user
     */
    class LocalUserSpringProperties {
        /**
         * The id of the user
         */
        var userId: Long? = null
        /**
         * The name of the user
         */
        var userName: String? = null
        /**
         * The password of the user
         */
        var password: String? = null
        /**
         * The profile of the user
         */
        var profile: String? = null
    }
}