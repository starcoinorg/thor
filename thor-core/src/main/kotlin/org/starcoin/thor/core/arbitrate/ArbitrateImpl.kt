package org.starcoin.thor.core.arbitrate

import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap

class ArbitrateImpl(private val periodMils: Long) :Arbitrate{
    private var winner = 0
    private var status: Status = Status.NOTOPEN
    private var timeLeft: Long = periodMils
    private var startTime: Long = 0
    private var users: Map<Int, Contract> = HashMap()

    init {
        this.status = Status.OPEN
    }

    override fun join(userId: Int, contract: Contract): Boolean {
        if (this.users.get(userId) != null) {
            return false
        }
        this.users.plus(Pair(userId, contract))
        return true
    }

    override fun challenge(proof: ContractInput) {
        var userId = proof.getUser()
        updateTimer()
        if (!checkTimer()) {
            this.status = Status.FINISH
            return
        }
        val contract = this.users.get(userId) ?: throw RuntimeException("User not join in arbitrate")
        contract.updateAll(proof)
        this.winner = contract.getWinner()!!
        if (userId != this.winner) {
            this.status = Status.FINISH
        }
    }

    override fun getLeftTime() = this.timeLeft

    override fun getWinner() = this.winner

    override fun getStatus() = this.status

    private fun current() = Calendar.getInstance().timeInMillis

    private fun updateTimer() {
        this.startTime = current()
    }

    private fun checkTimer() = current() - this.startTime >= periodMils
}

enum class Status {
    NOTOPEN,
    OPEN,
    FINISH,
}
