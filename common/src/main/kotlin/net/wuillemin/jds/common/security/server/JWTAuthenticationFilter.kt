package net.wuillemin.jds.common.security.server

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import net.wuillemin.jds.common.entity.Profile
import net.wuillemin.jds.common.exception.E
import net.wuillemin.jds.common.security.JWTConstant
import net.wuillemin.jds.common.security.UserPermission
import net.wuillemin.jds.common.service.LocalisationService
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.util.Assert
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.security.PublicKey
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Processes a HTTP request's JWT authorization headers, putting the result into the
 * `SecurityContextHolder`.
 *
 * If authentication is successful, the resulting Authentication object will be
 * placed into the `SecurityContextHolder`.
 *
 * If authentication fails and `ignoreFailure` is `false` (the
 * default), an AuthenticationEntryPoint implementation is called (unless the
 * <tt>ignoreFailure</tt> property is set to <tt>true</tt>). Usually this should be
 * a JWTAuthenticationEntryPoint], which will prompt the user to authenticate again
 * via JWT authentication.
 */


/**
 * Creates an instance which will authenticate against the supplied
 * `AuthenticationManager` and use the supplied `AuthenticationEntryPoint`
 * to handle authentication failures.
 *
 * @param publicKey The public key to be used to validate queries
 * @param localisationService The service for message translation
 * @param authenticationEntryPoint will be invoked when authentication fails.
 * Typically an instance of [JWTAuthenticationEntryPoint].
 */
open class JWTAuthenticationFilter(
    private val publicKey: PublicKey?,
    private val localisationService: LocalisationService,
    private val authenticationEntryPoint: AuthenticationEntryPoint? = null) : OncePerRequestFilter() {

    private val isIgnoreFailure = authenticationEntryPoint == null

    // Get the authentication and put it in the context
    private val defaultUserAuthentication = AuthenticationToken(
        "Default user",
        listOf(
            SimpleGrantedAuthority(Profile.ADMIN.springRoleName),
            SimpleGrantedAuthority(Profile.USER.springRoleName),
            SimpleGrantedAuthority(Profile.SERVICE.springRoleName)),
        UserPermission(
            "Default user",
            emptyList(),
            emptyList()))

    /**
     * Calls the {@code initFilterBean()} method that might
     * contain custom initialization of a subclass.
     * <p>Only relevant in case of initialization as bean, where the
     * standard {@code init(FilterConfig)} method won't be called.
     * @see #initFilterBean()
     * @see #init(javax.servlet.FilterConfig)
     */
    override fun afterPropertiesSet() {
        if (!isIgnoreFailure) {
            Assert.notNull(this.authenticationEntryPoint, "An AuthenticationEntryPoint is required")
        }
    }

    /**
     * Same contract as for {@code doFilter}, but guaranteed to be
     * just invoked once per request within a single request thread.
     * See {@link #shouldNotFilterAsyncDispatch()} for details.
     * <p>Provides HttpServletRequest and HttpServletResponse arguments instead of the
     * default ServletRequest and ServletResponse ones.
     */
    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain) {

        val debug = this.logger.isDebugEnabled

        publicKey
            ?.let { publicKey ->

                val header = request.getHeader("Authorization")

                if (header == null || !header.toLowerCase().startsWith("bearer ")) {
                    chain.doFilter(request, response)
                    return
                }

                try {
                    val tokenUserAndRoles = extractAndDecodeHeader(request, header, publicKey)

                    if (addAuthenticationIsRequired(tokenUserAndRoles.principal)) {

                        if (debug) {
                            this.logger.debug("Authentication success: '${tokenUserAndRoles.principal}'")
                        }

                        SecurityContextHolder.getContext().authentication = tokenUserAndRoles

                        onSuccessfulAuthentication(request, response, tokenUserAndRoles)
                    }

                }
                catch (failed: AuthenticationException) {
                    SecurityContextHolder.clearContext()

                    if (debug) {
                        this.logger.debug("Authentication request for failed: $failed")
                    }

                    onUnsuccessfulAuthentication(request, response, failed)

                    if (this.isIgnoreFailure) {
                        chain.doFilter(request, response)
                    }
                    else {
                        this.authenticationEntryPoint?.commence(request, response, failed)
                    }

                    return
                }
            }
            ?: run {
                SecurityContextHolder.getContext().authentication = defaultUserAuthentication
            }

        chain.doFilter(request, response)
    }

    /**
     * Decodes the header into a username and password.
     *
     * @throws BadCredentialsException if the JWT token is not present or is not valid
     */
    @Throws(IOException::class)
    private fun extractAndDecodeHeader(
        request: HttpServletRequest,
        header: String,
        publicKey: PublicKey): AuthenticationToken {

        val baseToken = header.substring(7)

        // Parse the token
        val jws: Jws<Claims> = try {
            // Read the token
            Jwts.parser()
                .setSigningKey(publicKey)
                .parseClaimsJws(baseToken)
            // we can safely trust the JWT
        }
        catch (e: ExpiredJwtException) {
            throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.expired, emptyArray(), request.locale))
        }
        catch (e: JwtException) {
            throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.malformedToken, arrayOf(e.message), request.locale), e)
        }

        // Others exceptions will be processed at higher level as we can not add more
        // information

        // Get the name of the user
        val userName = jws.body.subject

        // Get the roles of the user
        val rawRoles = jws.body[JWTConstant.ROLES_CLAIMS_NAME]
            ?: throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.rolesMissing, emptyArray(), request.locale))

        val roles = readStrings(
            rawRoles,
            localisationService.getMessage(E.security.jwtFilter.rolesNotList, emptyArray(), request.locale))

        if (roles.isEmpty()) {
            throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.rolesEmpty, emptyArray(), request.locale))
        }

        // Get the permission
        val rawPermission = jws.body[JWTConstant.PERMISSION_CLAIMS_NAME]
            ?: throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.permissionMissing, emptyArray(), request.locale))

        val permission = try {
            val permissionElements = rawPermission as? Map<*, *>
                ?: throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.permissionNotMap, emptyArray(), request.locale))

            val userId = permissionElements["userId"] as? String
                ?: throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.permissionMissingUserId, emptyArray(), request.locale))

            val adminGroupIds = readStrings(
                permissionElements["adminGroupIds"],
                localisationService.getMessage(E.security.jwtFilter.permissionMissingAdmin, emptyArray(), request.locale))

            val userGroupIds = readStrings(
                permissionElements["userGroupIds"],
                localisationService.getMessage(E.security.jwtFilter.permissionMissingUser, emptyArray(), request.locale))

            UserPermission(
                userId,
                adminGroupIds,
                userGroupIds)

        }
        catch (exception: Exception) {
            throw BadCredentialsException(localisationService.getMessage(E.security.jwtFilter.permissionOther, arrayOf(exception.message), request.locale), exception)
        }

        return AuthenticationToken(
            userName,
            roles.map { SimpleGrantedAuthority(it) },
            permission)
    }

    private fun addAuthenticationIsRequired(username: String): Boolean {
        // Only re-authenticate if username doesn't match SecurityContextHolder and user isn't authenticated
        val existingAuth = SecurityContextHolder.getContext().authentication

        if (existingAuth == null || !existingAuth.isAuthenticated) {
            return true
        }

        // Limit username comparison to providers which use user names (ie UsernamePasswordAuthenticationToken)
        if (existingAuth is AuthenticationToken && existingAuth.name != username) {
            return true
        }

        // Handle unusual condition where an AnonymousAuthenticationToken is already present
        // This shouldn't happen very often, as JWTAuthenticationFilter is meant to be earlier in the filter
        // chain than AnonymousAuthenticationFilter. Nevertheless, presence of both an AnonymousAuthenticationToken
        // together with a JWT authentication request header should indicate re-authentication using the
        // JWT protocol is desirable. This behaviour is also consistent with that provided by form and digest,
        // both of which force re-authentication if the respective header is detected (and in doing so replace
        // any existing AnonymousAuthenticationToken).
        return existingAuth is AnonymousAuthenticationToken
    }

    /**
     * Method to be overridden (if needed) and that will receive the event that an authentication was successful
     * @param request The servlet's request
     * @param response The servlet's response
     * @param authResult The authentication
     */
    @Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
    @Throws(IOException::class)
    protected fun onSuccessfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authResult: Authentication) {
    }

    /**
     * Method to be overridden (if needed) and that will receive the event that an authentication was raised an
     * exception
     * @param request The servlet's request
     * @param response The servlet's response
     * @param failed The exception raised
     */
    @Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
    @Throws(IOException::class)
    protected fun onUnsuccessfulAuthentication(
        request: HttpServletRequest,
        response: HttpServletResponse,
        failed: AuthenticationException) {
    }

    /**
     * A small helper method to read list of strings from the token
     */
    private fun readStrings(src: Any?, errorMessage: String): List<String> {

        // Get the object as a list
        val rawList = src as? List<*>
            ?: throw BadCredentialsException(errorMessage)

        // Convert its items
        return rawList.map {
            it
                ?.let(Any::toString)
                ?: run {
                    throw BadCredentialsException(errorMessage)
                }
        }
    }
}
