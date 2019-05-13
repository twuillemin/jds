package net.wuillemin.jds.common.security.server

import org.springframework.beans.factory.InitializingBean
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.util.Assert
import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Used by the `ExceptionTranslationFilter` to commence authentication via the
 * [JWTAuthenticationFilter].
 *
 *
 * Once a user agent is authenticated using BASIC authentication, logout requires that the
 * browser be closed or an unauthorized (401) header be sent. The simplest way of
 * achieving the latter is to call the [.commence]
 * method below. This will indicate to the browser its credentials are no longer
 * authorized, causing it to prompt the user to login again.
 */
class JWTAuthenticationEntryPoint : AuthenticationEntryPoint, InitializingBean {

    /**
     * The name of the realm
     */
    var realmName: String? = null


    /**
     * Invoked by the containing {@code BeanFactory} after it has set all bean properties
     * and satisfied {@link BeanFactoryAware}, {@code ApplicationContextAware} etc.
     * <p>This method allows the bean instance to perform validation of its overall
     * configuration and final initialization when all bean properties have been set.
     * @throws Exception in the event of misconfiguration (such as failure to set an
     * essential property) or if initialization fails for any other reason
     */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        Assert.hasText(realmName, "realmName must be specified")
    }

    /**
     * Commences an authentication scheme.
     * <p>
     * <code>ExceptionTranslationFilter</code> will populate the <code>HttpSession</code>
     * attribute named
     * <code>AbstractAuthenticationProcessingFilter.SPRING_SECURITY_SAVED_REQUEST_KEY</code>
     * with the requested target URL before calling this method.
     * <p>
     * Implementations should modify the headers on the <code>ServletResponse</code> as
     * necessary to commence the authentication process.
     *
     * @param request that resulted in an <code>AuthenticationException</code>
     * @param response so that the user agent can begin authentication
     * @param authException that caused the invocation
     *
     */
    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException) {

        response.addHeader("WWW-Authenticate", "JWT realm=\"$realmName\"")
        response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.reasonPhrase)
    }
}
