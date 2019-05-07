package org.starcoin.thor.core.arbitrate


abstract class Contract {
    abstract fun getSourceCode(): ByteArray
    abstract fun update(userId: String, state: ByteArray)
    abstract fun getWinner(): String
    fun updateAll(input: ContractInput): String? {
        input.forEach { update(it.userId(), it.data()) }
        return getWinner()
    }

    fun checkTimeout(input: ContractInput, timeLimitation: Long): String? {
        val inputs = input.asSequence().partition { it.userId() == input.getUser() }

        val timeEscapeSecond = inputs.second.map { it.timestamp() }.sortedByDescending { it }
        if (timeEscapeSecond.isEmpty() || timeEscapeSecond.reduce { a, b -> b!! - a!! }!! > timeLimitation) {
            return inputs.first.first().userId()
        }
        val timeEscapeFirst = inputs.first.map { it.timestamp() }.sortedByDescending { it }
        if (timeEscapeFirst.isEmpty() || timeEscapeFirst.reduce { a, b -> b!! - a!! }!! > timeLimitation) {
            return inputs.second.first().userId()
        }
        return null
    }
}