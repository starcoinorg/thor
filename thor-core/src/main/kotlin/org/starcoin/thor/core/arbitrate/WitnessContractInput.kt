package org.starcoin.thor.core.arbitrate

import com.google.common.primitives.Longs
import org.bitcoinj.core.Sha256Hash
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.thor.core.WitnessData
import org.starcoin.thor.utils.toHex
import java.security.PublicKey

data class WitnessContractInput(val userList: List<String>, val userId: String, val publicKeys: Triple<PublicKey, PublicKey, PublicKey>, val data: List<WitnessData>, var begin: Long = 0) : ContractInput {
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

                //verify hash
                if (flag) {
                    val hash = if (current == 0) {
                        Sha256Hash.of(Longs.toByteArray(begin)).bytes.toHEXString()
                    } else {
                        Sha256Hash.of(data[current - 1].data.bytes).bytes.toHEXString()
                    }

                    flag = (hash == data[current].stateHash.bytes.toHEXString())
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