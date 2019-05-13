package net.wuillemin.jds.common.security.utils

import net.wuillemin.jds.common.security.server.JWTAuthenticationFilterTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.nio.file.Paths

@ExtendWith(SpringExtension::class)
class CertificateFileReaderTest {

    private val pemFileReader = CertificateFileReader()

    @Test
    fun `Reader reads Private certificate`() {
        val privateKeyFile = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test1_priv.der").toURI())
        val privateKey = pemFileReader.readDERPrivateKey(privateKeyFile)

        assertThat(privateKey).isNotNull
    }

    @Test
    fun `Reader fails to read bad Private certificate`() {
        val privateKeyFile = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test1_pub.der").toURI())
        Assertions.assertThrows(Exception::class.java) { pemFileReader.readDERPrivateKey(privateKeyFile) }
    }

    @Test
    fun `Reader reads Public certificate`() {
        val publicKeyFile = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test1_pub.der").toURI())
        val publicKey = pemFileReader.readDERPublicKey(publicKeyFile)

        assertThat(publicKey).isNotNull
    }

    @Test
    fun `Reader fails to read bad Public certificate`() {
        val publicKeyFile = Paths.get(JWTAuthenticationFilterTest::class.java.classLoader.getResource("security/test1_priv.der").toURI())
        Assertions.assertThrows(Exception::class.java) { pemFileReader.readDERPublicKey(publicKeyFile) }
    }
}