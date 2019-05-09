package org.starcoin.thor.core.arbitrate

import kotlin.contracts.contract


abstract class Contract {
    abstract fun getSourceCode(): ByteArray
    abstract fun update(userId: String, state: String)
    abstract fun getWinner(): String
    fun updateAll(input: ContractInput): String? {
        input.forEach { update(it.userId(), it.data()) }
        return getWinner()
    }

    fun checkTimeout(input: ContractInput, timeLimitation: Long, startTime: Long): String {
        val inputs = input.asSequence().partition { it.userId() == input.getUser() }

        val rivalTimes = inputs.second.map { it.timestamp() }.sortedByDescending { it }
        var rivalTimeEscape = 0.toLong()
        rivalTimes.forEach {
            rivalTimeEscape += startTime - it!!
        }

        val selfTimes = inputs.first.map { it.timestamp() }.sortedByDescending { it }
        var selfTimeEscape = 0.toLong()
        selfTimes.forEach {
            selfTimeEscape += startTime - it!!
        }
        return if (rivalTimeEscape > selfTimeEscape) input.getUser() else inputs.second.first().userId()
    }
}