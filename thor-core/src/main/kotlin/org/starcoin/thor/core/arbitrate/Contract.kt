package org.starcoin.thor.core.arbitrate


abstract class Contract {
    abstract fun update(userId: Int, state: ByteArray)
    abstract fun getWinner(): Int?

    fun updateAll(input: ContractInput): Int? {
        input.forEach { update(input.getUser(), it) }
        return getWinner()
    }
}