package org.starcoin.thor.sign

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import org.bouncycastle.math.ec.FixedPointUtil
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.util.ByteUtil
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.util.error
import org.starcoin.thor.core.SignMsg
import org.starcoin.thor.core.WsMsg
import org.starcoin.thor.utils.decodeBase64
import org.starcoin.thor.utils.toBase64
import java.math.BigInteger
import java.security.*
import java.security.Security.addProvider
import java.util.*

interface SignService {
    fun generateKeyPair(): KeyPair
    fun sign(data: ByteArray, privateKey: PrivateKey): String

    fun signMessage(data: String, privateKey: PrivateKey): String

    fun verifySign(data: ByteArray, sign: String, publicKey: PublicKey): Boolean

    fun verifySignMessage(data: String, sign: String, publicKey: PublicKey): Boolean

    fun toPriKey(priKey: ByteArray): PrivateKey
    fun toPubKey(pubKey: ByteArray): PublicKey
    fun hexToPriKey(key: String): PrivateKey
    fun hexToPubKey(key: String): PublicKey
    fun hash(data: ByteArray): ByteArray
    fun hash160(data: ByteArray): ByteArray

    fun doVerify(msg: SignMsg, pubKey: PublicKey): Boolean {
        val json = msg.msg.toJson()
        LOG.fine("doVerify json: $json")
        return SignService.verifySign(json.toByteArray(), msg.sign, pubKey)
    }

    fun doSign(msg: WsMsg, priKey: PrivateKey): SignMsg {
        val json = msg.toJson()
        val bytes = json.toByteArray()
        val sign = SignService.sign(bytes, priKey)
        LOG.fine("doSign msg: $json, hex: ${bytes.toHEXString()} sign: $sign")
        return SignMsg(msg, sign)
    }

    companion object : SignService, WithLogging() {
        private val signService: SignService by lazy {
            val loaders = ServiceLoader.load(SignServiceProvider::class.java).iterator()
            if (loaders.hasNext()) {
                loaders.next().createSignService()
            } else {
                ECDSASignService()
            }
        }

        override fun generateKeyPair() = signService.generateKeyPair()
        override fun sign(data: ByteArray, privateKey: PrivateKey) = signService.sign(data, privateKey)
        override fun signMessage(data: String, privateKey: PrivateKey) = signService.signMessage(data, privateKey)
        override fun verifySign(data: ByteArray, sign: String, publicKey: PublicKey) = signService.verifySign(data, sign, publicKey)
        override fun verifySignMessage(data: String, sign: String, publicKey: PublicKey) = signService.verifySignMessage(data, sign, publicKey)
        override fun hexToPriKey(key: String) = signService.hexToPriKey(key)
        override fun hexToPubKey(key: String) = signService.hexToPubKey(key)
        override fun toPriKey(priKey: ByteArray) = signService.toPriKey(priKey)
        override fun toPubKey(pubKey: ByteArray) = signService.toPubKey(pubKey)
        override fun hash(data: ByteArray): ByteArray = signService.hash(data)
        override fun hash160(data: ByteArray): ByteArray = signService.hash160(data)
    }
}

interface SignServiceProvider {
    fun createSignService(): SignService
}

private const val ECDSA = "EC"

class ECDSASignService : SignService {

    companion object : WithLogging() {

        var parameterSpec: ECParameterSpec
        // The parameters of the secp256k1 curve that Bitcoin uses.
        private val CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1")

        /** The parameters of the secp256k1 curve that Bitcoin uses.  */
        var CURVE: ECDomainParameters
        private var HALF_CURVE_ORDER: BigInteger

        init {
            addProvider(BouncyCastleProvider())
            FixedPointUtil.precompute(CURVE_PARAMS.g)
            CURVE = ECDomainParameters(CURVE_PARAMS.curve, CURVE_PARAMS.g, CURVE_PARAMS.n,
                    CURVE_PARAMS.h)
            HALF_CURVE_ORDER = CURVE_PARAMS.n.shiftRight(1)
            parameterSpec = ECParameterSpec(CURVE_PARAMS.curve, CURVE_PARAMS.g, CURVE_PARAMS.n,
                    CURVE_PARAMS.h)
        }
    }

    override fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ECDSA, "BC")
        keyPairGenerator.initialize(parameterSpec)

        return keyPairGenerator.generateKeyPair()
    }

    override fun sign(data: ByteArray, privateKey: PrivateKey): String {
        val signature = privateKey.toECKey().sign(Sha256Hash.of(data))
        return signature.encode().toBase64()
    }

    override fun signMessage(data: String, privateKey: PrivateKey): String {
        return privateKey.toECKey().signMessage(data)
    }

    override fun verifySignMessage(data: String, sign: String, publicKey: PublicKey): Boolean {
        return try {
            val signBytes = sign.decodeBase64()
            //bitcoinjs signature is 64, miss header.
            val fixSign = if (signBytes.size == 64) {
                val newBytes = ByteArray(65)
                newBytes[0] = 27
                signBytes.copyInto(newBytes, 1, 0, 64)
                newBytes.toBase64()
            } else {
                sign
            }
            publicKey.toECKey().verifyMessage(data, fixSign)
            true
        } catch (e: Exception) {
            LOG.error(e)
            false
        }
    }

    override fun verifySign(data: ByteArray, sign: String, publicKey: PublicKey): Boolean {
        LOG.info("verifySign msg : ${data.toHEXString()}")
        val signature = sign.decodeBase64().deocdeSignature()
        val hash = Sha256Hash.of(data)
        LOG.info("verifySign msg hash: ${hash.bytes.toHEXString()}")
        return publicKey.toECKey().verify(hash, signature)
    }

    override fun hexToPriKey(key: String): PrivateKey {
        return toPriKey(key.hexToByteArray())
    }

    override fun hexToPubKey(key: String): PublicKey {
        return toPubKey(key.hexToByteArray())
    }

    override fun toPriKey(priKey: ByteArray): PrivateKey {
        val keyFactory = KeyFactory.getInstance(ECDSA, "BC")
        return keyFactory.generatePrivate(ECPrivateKeySpec(BigInteger(1, priKey), parameterSpec))
    }

    override fun toPubKey(pubKey: ByteArray): PublicKey {
        val point = CURVE.curve.decodePoint(pubKey)
        val pubSpec = ECPublicKeySpec(point, parameterSpec)
        val kf = KeyFactory.getInstance(ECDSA, "BC")
        return kf.generatePublic(pubSpec) as ECPublicKey
    }

    override fun hash(data: ByteArray): ByteArray {
        return Sha256Hash.hash(data)
    }

    override fun hash160(data: ByteArray): ByteArray {
        return Utils.sha256hash160(data)
    }
}

fun PublicKey.toHEX(): String {
    return this.toByteArray().toHEXString()
}

fun PublicKey.toByteArray(): ByteArray {
    if (this is ECPublicKey) {
        return this.q.getEncoded(true)
    }
    return this.encoded
}

fun PublicKey.getID(): String {
    return SignService.hash160(this.toByteArray()).toHEXString()
}

fun PublicKey.toECKey(): ECKey {
    return ECKey.fromPublicOnly(this.toByteArray())
}

fun PrivateKey.toByteArray(): ByteArray {
    val pk = this as BCECPrivateKey
    return ByteUtil.bigIntegerToBytes(pk.d, 32)
}

fun PrivateKey.toHEX(): String {
    return this.toByteArray().toHEXString()
}

fun PrivateKey.toECKey(): ECKey {
    return ECKey.fromPrivate(this.toByteArray())
}

fun ECKey.ECDSASignature.encode(): ByteArray {
    val sigData = ByteArray(64)
    System.arraycopy(Utils.bigIntegerToBytes(this.r, 32), 0, sigData, 0, 32)
    System.arraycopy(Utils.bigIntegerToBytes(this.s, 32), 0, sigData, 32, 32)
    return sigData
}

fun ByteArray.deocdeSignature(): ECKey.ECDSASignature {
    val r = BigInteger(1, Arrays.copyOfRange(this, 0, 32))
    val s = BigInteger(1, Arrays.copyOfRange(this, 32, 64))
    return ECKey.ECDSASignature(r, s)
}