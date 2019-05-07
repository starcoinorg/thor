package org.starcoin.thor.core.arbitrate

interface Arbitrate {
    fun join(userId: String, contract: Contract): Boolean
    fun challenge(proof: ContractInput)
    fun getWinner(): String
    fun getStatus(): Status
    fun getLeftTime(): Long
}