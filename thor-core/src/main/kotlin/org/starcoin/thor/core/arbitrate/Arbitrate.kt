package org.starcoin.thor.core.arbitrate

interface Arbitrate {
    // TODO: move contract init inside join
    fun join(user: Int, contract: Contract): Boolean
    fun challenge(proof: ContractInput)
    fun getWinner(): Int
    // TODO: Use deferred
    fun getStatus(): Status
    fun getLeftTime(): Long
}