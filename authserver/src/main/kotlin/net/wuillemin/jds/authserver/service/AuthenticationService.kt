package net.wuillemin.jds.authserver.service

import net.wuillemin.jds.authserver.config.AuthServerProperties
import net.wuillemin.jds.common.dto.RefreshRequest
import net.wuillemin.jds.common.dto.TokenRequest
import net.wuillemin.jds.common.dto.TokenResponse
import net.wuillemin.jds.common.entity.User
import net.wuillemin.jds.common.security.UserPermission
import net.wuillemin.jds.common.service.UserService
import org.slf4j.Logger
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

/**
 * Service processing the authentication, refresh, etc. request. This service mainly act as
 * a proxy to the TokenGenerator service
 *
 * @param permissionBuilder The service for building [UserPermission]
 * @param userService The service for managing [User]
 * @param tokenGenerator The service for generating Token
 * @param authServerProperties The properties of the authenticator
 * @param bCryptPasswordEncoder The component for hashing passwords
 * @param logger The logger
 */
@Service
class AuthenticationService(
    private val permissionBuilder: PermissionBuilder,
    private val userService: UserService,
    private val tokenGenerator: TokenGenerator,
    private val authServerProperties: AuthServerProperties,
    private val bCryptPasswordEncoder: BCryptPasswordEncoder,
    private val logger: Logger
) {

    /**
     * Process a loginByTokenRequest request. If the authentication can not be processed, an Exception
     * is thrown
     *
     * @param request The request to process
     * @return the authentication in a TokenResponse object if authentication is valid
     */
    fun loginByTokenRequest(request: TokenRequest): TokenResponse {

        logger.info("loginByTokenRequest: attempt authentication for ${request.userName}")

        val localUser = authServerProperties.localUsers.firstOrNull { it.userName == request.userName }

        // Get the profiles or throw an exception
        return localUser
            ?.let {
                // Local users have their password in clear
                if (it.password != request.password) {
                    logger.info("loginByTokenRequest: failed authentication for ${request.userName} due to Bad Password")
                }

                // Return a token with no specific permission are they should not really apply to
                // locally defined users
                tokenGenerator.buildToken(
                    it.userName,
                    listOf(it.profile),
                    UserPermission(
                        it.userId,
                        emptyList(),
                        emptyList()))

            }
            ?: run {
                // Will throw if user does not exist
                val user = userService.getUserByUserName(request.userName)

                // Users in database have their password b-encrypted
                if (!bCryptPasswordEncoder.matches(user.password, request.password)) {
                    logger.info("loginByTokenRequest: failed authentication for ${request.userName} due to Bad Password")
                }

                val userPermission = permissionBuilder.buildPermission(user)

                tokenGenerator.buildToken(
                    user.name,
                    listOf(user.profile.springRoleName),
                    userPermission)
            }
    }

    /**
     * Process a login without checking the password. This function is expected to be called from an endpoint
     * receiving an external authentication. LocalUsers can not be logged this way
     *
     * @param userName The name of the user
     * @return the authentication in a TokenResponse object if authentication is valid
     */
    fun loginFromExternalSource(userName: String): TokenResponse {

        logger.info("loginFromExternalSource: generate token for $userName")

        // Will throw if user does not exist
        val user = userService.getUserByUserName(userName)

        val userPermission = permissionBuilder.buildPermission(user)

        return tokenGenerator.buildToken(
            user.name,
            listOf(user.profile.springRoleName),
            userPermission)
    }

    /**
     * Process a refresh request. If the refresh can not be processed, an Exception
     * is thrown
     *
     * @param request The request to process
     * @return the refreshed authentication in a TokenResponse object if authentication is valid
     */
    fun refresh(request: RefreshRequest): TokenResponse {
        return tokenGenerator.refresh(request.refreshToken)
    }

    /**
     * Process a logout request. If the logout can not be processed, an Exception
     * is thrown
     *
     * @param request The request to process
     */
    fun logout(request: RefreshRequest) {
        tokenGenerator.logout(request.refreshToken)
    }
}
