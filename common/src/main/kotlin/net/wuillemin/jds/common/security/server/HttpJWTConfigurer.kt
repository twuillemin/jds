package net.wuillemin.jds.common.security.server

import net.wuillemin.jds.common.service.LocalisationService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.HttpSecurityBuilder
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.util.matcher.AndRequestMatcher
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.accept.ContentNegotiationStrategy
import org.springframework.web.accept.HeaderContentNegotiationStrategy
import java.security.PublicKey
import java.util.*

/**
 * Adds HTTP JWT based authentication. All attributes have reasonable defaults making
 * all parameters are optional. The following Filters are populated: [JWTAuthenticationFilter]
 *
 * @param localisationService The service for localisation of error messages
 * @param publicKey The public key for validating http queries
 */
class HttpJWTConfigurer<B : HttpSecurityBuilder<B>>(
    private val localisationService: LocalisationService,
    private val publicKey: PublicKey?) : AbstractHttpConfigurer<HttpJWTConfigurer<B>, B>() {

    companion object {
        private val X_REQUESTED_WITH = RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")
        private const val DEFAULT_REALM = "Realm"
    }

    private var authenticationEntryPoint: AuthenticationEntryPoint
    private val jwtAuthEntryPoint = JWTAuthenticationEntryPoint()

    init {
        realmName(DEFAULT_REALM)

        val entryPoints = LinkedHashMap<RequestMatcher, AuthenticationEntryPoint>()
        entryPoints[X_REQUESTED_WITH] = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)

        val defaultEntryPoint = DelegatingAuthenticationEntryPoint(entryPoints)
        defaultEntryPoint.setDefaultEntryPoint(this.jwtAuthEntryPoint)
        this.authenticationEntryPoint = defaultEntryPoint
    }

    /**
     * Allows easily changing the realm, but leaving the remaining defaults in place. If
     * [.authenticationEntryPoint] has been invoked,
     * invoking this method will result in an error.
     *
     * @param realmName the HTTP JWT realm to use
     * @return [HttpJWTConfigurer] for additional customization
     * @throws Exception
     */
    @Throws(Exception::class)
    @Suppress("MemberVisibilityCanBePrivate")
    fun realmName(realmName: String): HttpJWTConfigurer<B> {
        this.jwtAuthEntryPoint.realmName = realmName
        this.jwtAuthEntryPoint.afterPropertiesSet()
        return this
    }

    /**
     * The [AuthenticationEntryPoint] to be populated on
     * [JWTAuthenticationFilter] in the event that authentication fails. The
     * default to use [JWTAuthenticationEntryPoint] with the realm
     * "Realm".
     *
     * @param authenticationEntryPoint the [AuthenticationEntryPoint] to use
     * @return [HttpJWTConfigurer] for additional customization
     */
    @Suppress("unused")
    fun authenticationEntryPoint(authenticationEntryPoint: AuthenticationEntryPoint): HttpJWTConfigurer<B> {
        this.authenticationEntryPoint = authenticationEntryPoint
        return this
    }

    /**
     * Initialize the {@link SecurityBuilder}. Here only shared state should be created
     * and modified, but not properties on the {@link SecurityBuilder} used for building
     * the object. This ensures that the {@link #configure(SecurityBuilder)} method uses
     * the correct shared objects when building.
     *
     * @param http The builder
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun init(http: B?) {
        registerDefaults(http!!)
    }

    private fun registerDefaults(http: B) {
        var contentNegotiationStrategy: ContentNegotiationStrategy? = http.getSharedObject(ContentNegotiationStrategy::class.java)

        if (contentNegotiationStrategy == null) {
            contentNegotiationStrategy = HeaderContentNegotiationStrategy()
        }

        val restMatcher = MediaTypeRequestMatcher(
            contentNegotiationStrategy,
            MediaType.APPLICATION_ATOM_XML,
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_OCTET_STREAM,
            MediaType.APPLICATION_XML,
            MediaType.MULTIPART_FORM_DATA,
            MediaType.TEXT_XML)

        restMatcher.setIgnoredMediaTypes(setOf(MediaType.ALL))

        val allMatcher = MediaTypeRequestMatcher(contentNegotiationStrategy, MediaType.ALL)
        allMatcher.setUseEquals(true)

        val notHtmlMatcher = NegatedRequestMatcher(MediaTypeRequestMatcher(contentNegotiationStrategy, MediaType.TEXT_HTML))
        val restNotHtmlMatcher = AndRequestMatcher(Arrays.asList(notHtmlMatcher, restMatcher))

        val preferredMatcher = OrRequestMatcher(Arrays.asList(X_REQUESTED_WITH, restNotHtmlMatcher, allMatcher))

        registerDefaultEntryPoint(http, preferredMatcher)
        registerDefaultLogoutSuccessHandler(http, preferredMatcher)
    }

    private fun registerDefaultEntryPoint(http: B, preferredMatcher: RequestMatcher) {
        @Suppress("UNCHECKED_CAST")
        val clazz = ExceptionHandlingConfigurer::class.java as Class<out ExceptionHandlingConfigurer<B>>

        val exceptionHandling = http.getConfigurer(clazz) ?: return

        exceptionHandling.defaultAuthenticationEntryPointFor(postProcess<AuthenticationEntryPoint>(this.authenticationEntryPoint), preferredMatcher)
    }

    private fun registerDefaultLogoutSuccessHandler(http: B, preferredMatcher: RequestMatcher) {
        @Suppress("UNCHECKED_CAST")
        val clazz = LogoutConfigurer::class.java as Class<out LogoutConfigurer<B>>

        val logout = http.getConfigurer(clazz) ?: return

        logout.defaultLogoutSuccessHandlerFor(postProcess(HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)), preferredMatcher)
    }

    /**
     * Configure the {@link SecurityBuilder} by setting the necessary properties on the
     * {@link SecurityBuilder}.
     *
     * @param http the builder
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun configure(http: B) {
        var jwtAuthenticationFilter = JWTAuthenticationFilter(publicKey, localisationService, this.authenticationEntryPoint)
        jwtAuthenticationFilter = postProcess(jwtAuthenticationFilter)
        http.addFilterAfter(jwtAuthenticationFilter, LogoutFilter::class.java)
    }
}
