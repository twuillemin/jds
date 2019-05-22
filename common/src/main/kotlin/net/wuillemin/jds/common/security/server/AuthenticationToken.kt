package net.wuillemin.jds.common.security.server

import net.wuillemin.jds.common.entity.Loggable
import net.wuillemin.jds.common.security.UserPermission
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.SpringSecurityCoreVersion

/**
 * An [org.springframework.security.core.Authentication] implementation that is
 * designed for simple presentation of a username and password.
 *
 *
 * The `principal` and `credentials` should be set with an
 * [Object] that provides the respective property via its
 * `Object.toString()` method. The simplest such `Object` to use is
 * [String].
 *
 * @param principal The name of the user (principal in Spring dialect)
 * @param authorities The authorities (roles) of the user
 * @param permission The authorities (roles) of the user
 */
open class AuthenticationToken(
    private val principal: String,
    authorities: Collection<GrantedAuthority>,
    val permission: UserPermission
) : AbstractAuthenticationToken(authorities), Loggable {

    companion object {
        private const val serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID
    }

    init {
        super.setAuthenticated(true)
    }

    /**
     * The credentials that prove the principal is correct. This is usually a password,
     * but could be anything relevant to the <code>AuthenticationManager</code>. Callers
     * are expected to populate the credentials.
     *
     * @return the credentials that prove the identity of the <code>Principal</code>
     */
    override fun getCredentials(): Any? {
        return "JWT"
    }

    /**
     * The identity of the principal being authenticated. In the case of an authentication
     * request with username and password, this would be the username. Callers are
     * expected to populate the principal for an authentication request.
     * <p>
     * The <tt>AuthenticationManager</tt> implementation will often return an
     * <tt>Authentication</tt> containing richer information as the principal for use by
     * the application. Many of the authentication providers will create a
     * {@code UserDetails} object as the principal.
     *
     * @return the <code>Principal</code> being authenticated or the authenticated
     * principal after authentication.
     */
    override fun getPrincipal(): String {
        return this.principal
    }

    /**
     * See {@link #isAuthenticated()} for a full description.
     * <p>
     * Implementations should <b>always</b> allow this method to be called with a
     * <code>false</code> parameter, as this is used by various classes to specify the
     * authentication token should not be trusted. If an implementation wishes to reject
     * an invocation with a <code>true</code> parameter (which would indicate the
     * authentication token is trusted - a potential security risk) the implementation
     * should throw an {@link IllegalArgumentException}.
     *
     * @param isAuthenticated <code>true</code> if the token should be trusted (which may
     * result in an exception) or <code>false</code> if the token should not be trusted
     *
     * @throws IllegalArgumentException if an attempt to make the authentication token
     * trusted (by passing <code>true</code> as the argument) is rejected due to the
     * implementation being immutable or implementing its own alternative approach to
     * {@link #isAuthenticated()}
     */
    @Throws(IllegalArgumentException::class)
    override fun setAuthenticated(isAuthenticated: Boolean) {
        if (isAuthenticated) {
            throw IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead")
        }

        super.setAuthenticated(false)
    }

    override fun getLoggingId(): String {
        return "'${this.principal} [id: ${this.permission.userId}]'"
    }
}