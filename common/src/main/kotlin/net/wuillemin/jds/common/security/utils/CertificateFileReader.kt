package net.wuillemin.jds.common.security.utils

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


/**
 * Helper class for reading PEM File
 */
class CertificateFileReader {

    /**
     * Read a private key from a DER file. The key must be given at PKCS#8 format
     *
     * @param derFile The file
     * @return The private key
     */
    fun readDERPrivateKey(derFile: Path): PrivateKey {
        val keyBytes = Files.readAllBytes(derFile)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    /**
     * Read a public key from a DER file.
     *
     * @param derFile The file
     * @return The public key
     */
    fun readDERPublicKey(derFile: Path): PublicKey {
        val keyBytes = Files.readAllBytes(derFile)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec)
    }
}


