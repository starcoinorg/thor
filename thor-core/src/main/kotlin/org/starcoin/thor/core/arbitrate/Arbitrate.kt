package org.starcoin.thor.core.arbitrate

import java.util.*

class Arbitrate(private val contract: Contract, private val periodMils: Long) {
    private var winner = 0
    private var status: Status = Status.NOTOPEN
    private var timeleft: Long = periodMils
    private var startTime: Long = 0

    init {
        this.status = Status.OPEN
    }

    fun challenge(userId: Int, proof: ContractInput) {
        updateTimer()
        if (!checkTimer()) {
            this.status = Status.FINISH
            return
        }

        contract.updateAll(proof)

        this.winner = contract.getWinner()!!
        if (userId != this.winner) {
            this.status = Status.FINISH
        }
    }

    fun getLeftTime() = this.timeleft

    fun getWinner() = this.winner

    fun getStatus() = this.status

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
