package org.statcoin.thor.test.sign

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.lang.hexToByteArray
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.toByteArray
import org.starcoin.thor.sign.toECKey
import org.starcoin.thor.sign.toHEX
import org.starcoin.thor.utils.decodeBase64

class SignTest {

    @Test
    fun testSign() {
        val key = SignService.generateKeyPair()

        val str = "ssssss".toByteArray()
        val sign = SignService.sign(str, key.private)

        assert(SignService.verifySign(str, sign, key.public))
    }

    @Test
    fun testString() {
        val key = SignService.generateKeyPair()
        val pubStr = key.public.toHEX()
        val priStr = key.private.toHEX()

        assert(key.public == SignService.hexToPubKey(pubStr))
        assert(key.private == SignService.hexToPriKey(priStr))
    }

    @Test
    fun testPublicKey() {
        val key = SignService.generateKeyPair()
        val bytes = key.public.toByteArray()
        println(bytes.size)
        val key2 = SignService.toPubKey(bytes)
        Assert.assertArrayEquals(bytes, key2.toByteArray())
    }

    @Test
    fun testPublicString() {
        val pubKeyStr = "0x02ee56571afcbd565eff25c95eb30eaf3438acc507c07ace063a8edb40788c6e04"
        val bytes = pubKeyStr.hexToByteArray()
        println(bytes.size)
        val key = SignService.toPubKey(bytes)
        Assert.assertEquals(pubKeyStr, key.toHEX())
    }

    @Test
    fun testECKey() {
        val key = SignService.generateKeyPair()
        val ecKey = key.private.toECKey();
        println(ecKey)
    }

    @Test
    fun testMsgSignature() {
        val key = SignService.generateKeyPair()
        val signature = SignService.signMessage("test", key.private)
        val signatureBytes = signature.decodeBase64()
        println(signatureBytes.size)
    }

    @Test
    fun testHex() {
        val hex = "a22303235383239336266623834386434353534343538366539316438636330356136386362373663333634653932623730333233633631653238623262373438343962227d7d"
        val str = hex.hexToByteArray().toString(Charsets.UTF_8)
        println(str)
    }
}