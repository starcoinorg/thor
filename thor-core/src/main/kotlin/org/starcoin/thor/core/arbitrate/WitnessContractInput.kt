package org.starcoin.thor.core.arbitrate

import org.starcoin.thor.core.WitnessData
import java.security.PublicKey

data class WitnessContractInput(val userList: List<String>, val userId: String, val publicKeys: Triple<PublicKey, PublicKey, PublicKey>, val data: List<WitnessData>) : ContractInput {
    private var current = 0
    private val size = data.size
    private var flag = true

    override fun getUser(): String {
        return this.userId
    }

    override fun hasNext(): Boolean {
        return synchronized(this) {
            if (current < size && flag) {
                val first = ((current % 2) == 0)

                val tmpPk = if (first) {
                    publicKeys.second
                } else {
                    publicKeys.third
                }
                flag = data[current].checkSign(tmpPk)
                if (flag) {
                    flag = data[current].checkArbiterSign(publicKeys.first)
                }
            }
            current < size && flag
        }
    }

    override fun reset() {
        synchronized(this) {
            current = 0
        }
    }

    override fun next(): ArbitrateData {
        synchronized(this) {
            val arbitrateData = ArbitrateDataImpl(userList, data[current])
            current = ++current
            return arbitrateData
        }
    }
}