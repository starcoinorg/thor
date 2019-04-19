package org.statcoin.thor.test.sign

import org.junit.Test
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.toBase64

class SignTest {

    @Test
    fun testSign() {
        val key = SignService.generateKeyPair()

        val str = "ssssss".toByteArray()
        val sign = SignService.sign(str, "", key.private)

        assert(SignService.verifySign(str, sign, key.public))
    }

    @Test
    fun testString() {
        val key = SignService.generateKeyPair()
        val pubStr = key.public.toBase64()
        val priStr = key.private.toBase64()

        assert(key.public == SignService.base64ToPubKey(pubStr))
        assert(key.private == SignService.base64ToPriKey(priStr))
    }
}