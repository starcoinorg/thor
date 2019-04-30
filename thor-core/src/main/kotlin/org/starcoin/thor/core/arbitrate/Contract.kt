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

        val timeEscapeFirst = inputs.first.map { it.timestamp() }.sortedByDescending { it }.reduce { a, b -> b!! - a!! }
        val timeEscapeSecond = inputs.second.map { it.timestamp() }.sortedByDescending { it }.reduce { a, b -> b!! - a!! }
        if (timeEscapeSecond!! > timeLimitation) {
            return inputs.second.first().userId()
        }
        if (timeEscapeFirst!! > timeLimitation) {
            return inputs.first.first().userId()
        }
        return null
    }
}