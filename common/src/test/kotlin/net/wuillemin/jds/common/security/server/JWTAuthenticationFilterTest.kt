package net.wuillemin.jds.common.security.server

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.jsonwebtoken.Jwts
import net.wuillemin.jds.common.security.JWTConstant
import net.wuillemin.jds.common.security.UserPermission
import net.wuillemin.jds.common.security.utils.CertificateFileReader
import net.wuillemin.jds.common.service.LocalisationService
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.context.MessageSource
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Paths
import java.time.Instant
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@ExtendWith(SpringExtension::class)
class JWTAuthenticationFilterTest {

    private val pemFileReader = CertificateFileReader()

    private val privateKeyFile = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test1_priv.der").toURI())
    private val privateKey = pemFileReader.readDERPrivateKey(privateKeyFile)

    private val privateKeyFileBad = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test2_priv.der").toURI())
    private val privateKeyBad = pemFileReader.readDERPrivateKey(privateKeyFileBad)

    private val publicKeyFile = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test1_pub.der").toURI())
    private val publicKey = pemFileReader.readDERPublicKey(publicKeyFile)

    private val dummyPermission = UserPermission(
        -1L,
        emptyList(),
        emptyList())

    private val messageSource = mock(MessageSource::class.java)
    private val localisationService = LocalisationService(messageSource)


    @BeforeEach
    fun beforeEach() {
        whenever(messageSource.getMessage(anyOrNull(), any(), any())).thenReturn("dummy test message")
    }

    @Test
    fun `Valid request and token JWT authenticate the user`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, listOf("USER"))
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNotNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Token with empty internal claims does not authenticate the user`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, emptyList<String>())
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Expired token does not authenticate the user`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.minusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, listOf("USER"))
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Token signed by other CA JWT does not authenticate the user`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, listOf("USER"))
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKeyBad)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Request with missing Authorization does not authenticate the user`() {

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(null)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Request with Basic Authorization does not authenticate the user`() {

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn("Basic dXNlcjpwYXNzd29yZA==")
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Malformed token does not authenticate the user`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, listOf("USER"))
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token.subSequence(0, 20))
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Token with missing internal claims does not authenticate the user-1`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Token with missing internal claims does not authenticate the user-2`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, listOf("USER"))
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `Token with malformed claims does not authenticate the user`() {

        // Make a valid token
        val currentDate = Instant.now()
        val token = Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .setIssuer("JDS Authentication Server")
            .setAudience("JDS")
            .setSubject("userName")
            .setIssuedAt(Date.from(currentDate))
            .setNotBefore(Date.from(currentDate))
            .setExpiration(Date.from(currentDate.plusSeconds(600)))
            .claim(JWTConstant.ROLES_CLAIMS_NAME, "That should not work")
            .claim(JWTConstant.PERMISSION_CLAIMS_NAME, dummyPermission)
            .signWith(privateKey)
            .compact()

        // Build the filter
        val filter = JWTAuthenticationFilter(publicKey, localisationService)

        // Call the filter
        val request = mock(HttpServletRequest::class.java)
        whenever(request.getHeader(eq(JWTConstant.HTTP_HEADER_AUTHORIZATION))).thenReturn(JWTConstant.HTTP_HEADER_BEARER_PREFIX + token)
        whenever(request.locale).thenReturn(Locale.getDefault())

        val response = mock(HttpServletResponse::class.java)

        val chain = mock(FilterChain::class.java)

        filter.doFilter(
            request,
            response,
            chain)

        verify(chain, times(1)).doFilter(any(), any())
        assertNull(SecurityContextHolder.getContext().authentication)
    }
}