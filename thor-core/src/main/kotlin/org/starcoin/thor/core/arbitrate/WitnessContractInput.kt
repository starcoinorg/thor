package org.starcoin.thor.core.arbitrate

import org.starcoin.thor.core.WitnessData
import java.security.PublicKey

data class WitnessContractInput(val userId: Int, val publicKeys: Triple<PublicKey, PublicKey, PublicKey>, val data: List<WitnessData>) : ContractInput {
    private var current = 0
    private val size = data.size
    private var flag = true

    override fun getUser(): Int {
        return this.userId
    }

    override fun hasNext(): Boolean {
        return synchronized(this) {
            //TODO("verify sign and set flag")
            current < size && flag
        }
    }

    override fun next(): ByteArray {
        synchronized(this) {
            val d = data[current].data.bytes
            current = current++
            return d
        }
    }
}