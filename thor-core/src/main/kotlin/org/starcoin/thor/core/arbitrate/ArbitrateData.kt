package org.starcoin.thor.core.arbitrate

import org.starcoin.thor.core.WitnessData
import org.starcoin.thor.utils.toHex

interface ArbitrateData {
    fun data(): String
    fun timestamp(): Long?
    fun userId(): String
}


class ArbitrateDataImpl(private val userList: List<String>, private val witnessData: WitnessData) : ArbitrateData {
    override fun data(): String {
        return witnessData.data.toHex()
    }

    override fun timestamp(): Long? {
        return witnessData.timestamp
    }

    override fun userId(): String {
        return (userList.indexOf(witnessData.userId) + 1).toString()
    }
}