package org.starcoin.thor.server


import kotlin.random.Random
import org.starcoin.lightning.client.HashUtils

class Preimage {
    private var preimage: ByteArray = ByteArray(32)

    init {
        preimage = Random.nextBytes(preimage)
    }

    fun hex(): String {
        return HashUtils.bytesToHex(preimage)
    }

    fun hash(): ByteArray {
        return HashUtils.hash160(preimage)
    }
}
