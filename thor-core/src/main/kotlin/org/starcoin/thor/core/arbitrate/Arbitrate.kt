package org.starcoin.thor.core.arbitrate

interface Arbitrate {
    fun join(user: Int, contract: Contract): Boolean
    fun challenge(proof: ContractInput)
    fun getWinner(): Int
    // TODO: Use deferred
    fun getStatus(): Status
    fun getLeftTime(): Long
}