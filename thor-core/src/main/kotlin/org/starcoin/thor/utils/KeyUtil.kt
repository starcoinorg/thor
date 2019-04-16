package org.starcoin.thor.utils

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.util.encoders.Base64
import java.io.DataInputStream
import java.io.File
import java.security.KeyPairGenerator
import java.security.spec.X509EncodedKeySpec
import java.io.FileInputStream
import java.security.Key
import java.security.KeyFactory


class KeyUtil {

    fun test() {
//        val f = File("")
//        val fis = FileInputStream(f)
//        val dis = DataInputStream(fis)
//        val keyBytes = ByteArray(f.length().toInt())
//        dis.readFully(keyBytes)
//        dis.close()
//
//        val temp = String(keyBytes)
//        var privKeyPEM = temp.replace("-----BEGIN PRIVATE KEY-----\n", "")
//        privKeyPEM = privKeyPEM.replace("-----END PRIVATE KEY-----", "")
//        val decoded = Base64.decode(privKeyPEM)
//
//        val spec = X509EncodedKeySpec(keyBytes)
//        val kf = KeyFactory.getInstance("RSA")
//        pk = kf.generatePublic(spec)
//
//
//        val ecSpec = ECNamedCurveTable.getParameterSpec("B-571")
//        val g = KeyPairGenerator.getInstance("ECDSA", "BC")
//
//        g.initialize(ecSpec, new SecureRandom ());
//        KeyPair keypair = g . generateKeyPair ();
//        String publicKey = keypair . getPublic ();
//        String privateKey = keypair . getPrivate ();
    }
}