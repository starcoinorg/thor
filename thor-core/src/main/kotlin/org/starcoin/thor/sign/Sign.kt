package org.starcoin.thor.sign

import org.starcoin.thor.core.SignMsg
import org.starcoin.thor.core.WsMsg
import java.util.*
import sun.misc.BASE64Encoder
import sun.misc.BASE64Decoder
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.spec.PKCS8EncodedKeySpec

interface SignService {
    fun generateKeyPair(): KeyPair
    //    fun loadKeyPair(path:String): KeyPair//TODO()
    fun sign(data: ByteArray, pwd: String, privateKey: PrivateKey): String

    fun verifySign(data: ByteArray, sign: String, publicKey: PublicKey): Boolean
    fun toPriKey(key: ByteArray): PrivateKey
    fun toPubKey(key: ByteArray): PublicKey
    fun base64ToPriKey(key: String): PrivateKey
    fun base64ToPubKey(key: String): PublicKey

    companion object : SignService {
        private val signService: SignService by lazy {
            val loaders = ServiceLoader.load(SignServiceProvider::class.java).iterator()
            if (loaders.hasNext()) {
                loaders.next().createSignService()
            } else {
                ECDSASignService()
            }
        }

        override fun generateKeyPair() = signService.generateKeyPair()
        override fun sign(data: ByteArray, pwd: String, privateKey: PrivateKey) = signService.sign(data, pwd, privateKey)
        override fun verifySign(data: ByteArray, sign: String, publicKey: PublicKey) = signService.verifySign(data, sign, publicKey)
        override fun base64ToPriKey(key: String) = signService.base64ToPriKey(key)
        override fun base64ToPubKey(key: String) = signService.base64ToPubKey(key)
        override fun toPriKey(key: ByteArray) = signService.toPriKey(key)
        override fun toPubKey(key: ByteArray) = signService.toPubKey(key)
    }
}

interface SignServiceProvider {
    fun createSignService(): SignService
}

private const val SIGN_ALGORITHM = "SHA256withECDSA"
private const val MD5 = "MD5"
private const val ECDSA = "EC"
private const val SHA1PRNG = "SHA1PRNG"
private const val size = 256
private const val SECP = "secp256k1"

class ECDSASignService : SignService {
    override fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ECDSA)
        keyPairGenerator.initialize(ECGenParameterSpec(SECP))
        keyPairGenerator.initialize(size, SecureRandom.getInstance(SHA1PRNG))

        return keyPairGenerator.generateKeyPair()
    }

    override fun sign(data: ByteArray, pwd: String, privateKey: PrivateKey): String {
        val signature = Signature.getInstance(SIGN_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(data)
        return BASE64Encoder().encode(signature.sign())
    }

    override fun verifySign(data: ByteArray, sign: String, publicKey: PublicKey): Boolean {
        val signature = Signature.getInstance(SIGN_ALGORITHM)
        signature.initVerify(publicKey)
        signature.update(data)
        return signature.verify(BASE64Decoder().decodeBuffer(sign))
    }

    override fun base64ToPriKey(key: String): PrivateKey {
        val priKey = BASE64Decoder().decodeBuffer(key)
        return toPriKey(priKey)
    }

    override fun base64ToPubKey(key: String): PublicKey {
        val pubKey = BASE64Decoder().decodeBuffer(key)
        return toPubKey(pubKey)
    }

    override fun toPriKey(priKey: ByteArray): PrivateKey {
        val spec = PKCS8EncodedKeySpec(priKey)
        val keyFactory = KeyFactory.getInstance(ECDSA)
        return keyFactory.generatePrivate(spec)
    }

    override fun toPubKey(pubKey: ByteArray): PublicKey {
        val spec = X509EncodedKeySpec(pubKey)
        val keyFactory = KeyFactory.getInstance(ECDSA)
        return keyFactory.generatePublic(spec)
    }
}

fun PublicKey.toBase64(): String {
    return BASE64Encoder().encode(this.encoded)
}

fun PublicKey.getID(): String {
    val md = MessageDigest.getInstance(MD5)
    md.update(this.encoded)
    return BASE64Encoder().encode(md.digest())
}

fun PrivateKey.toBase64(): String {
    return BASE64Encoder().encode(this.encoded)
}

fun SignService.doVerify(msg: SignMsg, pubKey: PublicKey): Boolean {
    return SignService.verifySign(msg.msg.toJson().toByteArray(), msg.sign, pubKey)
}

fun SignService.doSign(msg: WsMsg, priKey: PrivateKey): SignMsg {
    val sign = SignService.sign(msg.toJson().toByteArray(), "", priKey)
    return SignMsg(msg, sign)
}