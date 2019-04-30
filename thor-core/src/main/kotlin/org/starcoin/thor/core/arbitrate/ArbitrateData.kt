package org.starcoin.thor.core.arbitrate

import org.starcoin.thor.core.WitnessData

interface ArbitrateData {
    fun data(): ByteArray
    fun timestamp(): Long?
    fun userId(): String
}


class ArbitrateDataImpl(private val witnessData: WitnessData) : ArbitrateData {
    override fun data(): ByteArray {
        return witnessData.data.bytes
    }

    override fun timestamp(): Long? {
        return witnessData.timestamp
    }
    override fun userId(): String {
        return witnessData.userId
    }
}