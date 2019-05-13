package net.wuillemin.jds.authserver.service

import io.jsonwebtoken.Jwts
import net.wuillemin.jds.authserver.config.AuthServerProperties
import net.wuillemin.jds.authserver.exception.E
import net.wuillemin.jds.common.dto.TokenResponse
import net.wuillemin.jds.common.exception.AuthenticationRejectedException
import net.wuillemin.jds.common.exception.BadParameterException
import net.wuillemin.jds.common.security.JWTConstant.Companion.PERMISSION_CLAIMS_NAME
import net.wuillemin.jds.common.security.JWTConstant.Companion.ROLES_CLAIMS_NAME
import net.wuillemin.jds.common.security.UserPermission
import net.wuillemin.jds.common.service.UserService
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

/**
 * Service for building token
 *
 * @param permissionBuilder The builder of permissions
 * @param userService The service for getting user information
 * @param authServerProperties The properties of the server
 */
@Service
class TokenGenerator(
    private val permissionBuilder: PermissionBuilder,
    private val userService: UserService,
    private val authServerProperties: AuthServerProperties) {

    private val refreshCache: MutableMap<UUID, RefreshCacheEntry> = HashMap()

    /**
     * Build a new token for the user. This function returns all the information (token, refresh, etc.)
     * in a single object. Creating a token will also register it in the Refresher service.
     *
     * @param userName The name (the id) of the user
     * @param roles The roles of the user
     * @param userPermission The rights of the user on the model object
     *
     * @return a TokenResponse object having all the needed information
     */
    fun buildToken(
        userName: String,
        roles: List<String>,
        userPermission: UserPermission): TokenResponse {

        val currentDate = Instant.now()

        val authenticationToken = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject(userName)
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(authServerProperties.tokenTimeToLiveInSeconds)))
            .claim(ROLES_CLAIMS_NAME, roles)
            .claim(PERMISSION_CLAIMS_NAME, userPermission)
            .signWith(authServerProperties.privateKey)
            .compact()

        val authenticationTokenExpiration = currentDate.epochSecond + authServerProperties.tokenTimeToLiveInSeconds

        val refreshToken = UUID.randomUUID()

        val refreshTokenExpiration = currentDate.epochSecond + authServerProperties.refreshTimeToLiveInSeconds

        // Add the refresh token to the refresher
        refreshCache[refreshToken] = RefreshCacheEntry(
            refreshToken,
            Instant.ofEpochSecond(refreshTokenExpiration),
            userName,
            roles,
            userPermission)

        // Make the response
        return TokenResponse(
            authenticationToken,
            authenticationTokenExpiration,
            refreshToken.toString(),
            refreshTokenExpiration)
    }

    /**
     * Build a new token for the refreshing token. This function returns all the information (token, refresh, etc.)
     * in a single object. Refreshing a token will also register it in the Refresher service. If the given token does
     * not exist or is invalid, an exception is thrown
     *
     * @param refreshToken The existing refreshing token
     *
     * @return a TokenResponse object having all the needed information
     *
     * @throws AuthenticationRejectedException if the given token does not exist in the cache or is expired
     * @throws BadParameterException if the token is malformed
     */
    fun refresh(refreshToken: String): TokenResponse {

        val tokenId = try {
            UUID.fromString(refreshToken)
        }
        catch (e: IllegalArgumentException) {
            throw BadParameterException(E.service.tokenGenerator.malformedToken, refreshToken)
        }

        // Get the previous token
        val refreshCacheEntry = refreshCache[tokenId]
            ?: throw AuthenticationRejectedException(E.service.tokenGenerator.tokenNotFound)

        // Check the time
        if (refreshCacheEntry.tokenExpirationData.isBefore(Instant.now())) {
            removeTokenFromCacheWithGraceDelay(tokenId)
            throw AuthenticationRejectedException(E.service.tokenGenerator.tokenExpired)
        }

        // Remade a new token
        val newToken = buildToken(
            refreshCacheEntry.userName,
            refreshCacheEntry.roles,
            refreshCacheEntry.userPermission)

        // Remove the old token from cache
        removeTokenFromCacheWithGraceDelay(tokenId)

        return newToken
    }

    /**
     * Logout the user. Simply removes the refresh id given from the list of refreshable.
     * As there is no way of un-validating a token, this allows to disable the refresh for
     * the user
     *
     * @param refreshToken The existing refreshing token
     *
     * @throws BadParameterException if the token is malformed
     */
    fun logout(refreshToken: String) {
        val tokenId = try {
            UUID.fromString(refreshToken)
        }
        catch (e: IllegalArgumentException) {
            throw BadParameterException(E.service.tokenGenerator.malformedToken, refreshToken)
        }

        // Remove the previous token
        // If not present, do not raise an error as it could have deleted by another event
        refreshCache.remove(tokenId)
    }

    /**
     * Search all token for the user and remove them, so that the user will have to reconnect
     *
     * @param userId The id of the user to be removed
     *
     */
    fun logOutAllUserSessions(userId: String) {

        val keyToBeRemoved = refreshCache.entries
            .filter { it.value.userPermission.userId == userId }
            .map { it.key }

        keyToBeRemoved.forEach {
            refreshCache.remove(it)
        }

    }

    /**
     * Search all token for the user and update their permissions
     *
     * @param userId The id of the user to be removed
     */
    fun updateUserPermission(userId: String) {

        // Get the user
        val user = userService.getUserById(userId)

        // Rebuild its permission
        val permission = permissionBuilder.buildPermission(user)

        // Find all sessions to be updated
        val keyToBeUpdated = refreshCache.entries
            .filter { it.value.userPermission.userId == userId }
            .map { it.key }

        // Update the entry in the cache
        keyToBeUpdated.forEach { id ->
            refreshCache[id]
                ?.let { currentEntry ->
                    refreshCache[id] = currentEntry.copy(
                        userName = user.userName,
                        roles = listOf(user.profile.toString()),
                        userPermission = permission)
                }
        }
    }

    /**
     * Remove a tokenId from the cache after one minute
     *
     * @param tokenId The token to be removed
     */
    private fun removeTokenFromCacheWithGraceDelay(tokenId: UUID) {

        // Create a new task for removing the old tokenId
        val task = object : TimerTask() {
            override fun run() {
                refreshCache.remove(tokenId)
            }
        }

        // Will remove the old tokenId in 1 minute
        Timer("Invalidate " + tokenId.toString())
            .schedule(task, 60000)
    }

    /**
     * Internal class for caching the current authentication
     */
    private data class RefreshCacheEntry(
        val refreshToken: UUID,
        val tokenExpirationData: Instant,
        val userName: String,
        val roles: List<String>,
        val userPermission: UserPermission)
}