package org.starcoin.thor.core.arbitrate

import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashMap
import kotlinx.coroutines.*

class ArbitrateImpl(periodMils: Long, finishNotify: (winner: String) -> Unit) : Arbitrate {
    private var winner: String = "0"
    private var status: Status = Status.NOTOPEN
    private var users: MutableMap<String, Contract> = HashMap()
    private val timer = Timer(periodMils) { println("the winner is ${this.winner}");finishNotify(this.winner) }

    init {
        this.status = Status.OPEN
        timer.start()
    }

    override fun join(userId: String, contract: Contract): Boolean {
        if (this.users[userId] != null) {
            return false
        }

        this.users[userId] = contract
        return true
    }

    override fun challenge(proof: ContractInput) {
        val userId = proof.getUser()
        val contract = this.users[userId]
                ?: throw RuntimeException("User not join in arbitrate")
        if (timer.getLeftTime() == 0.toLong()) {
            this.status = Status.FINISH
            return
        }

        contract.updateAll(proof)
        this.winner = contract.getWinner()
        if (this.winner != "0") {
            this.status = Status.FINISH
            return
        }
        proof.reset()
        val timeoutUser = contract.checkTimeout(proof, timer.startTime)
        this.status = Status.FINISH
        this.winner = timeoutUser
    }

    override fun getLeftTime() = this.timer.getLeftTime()

    override fun getWinner() = this.winner

    override fun getStatus() = this.status
}

enum class Status {
    NOTOPEN,
    OPEN,
    FINISH,
}

class Timer(periodMils: Long, private val notify: () -> Unit) {
    private var leftTime: Long = periodMils
    var startTime: Long = 0

    fun start() {
        startTime = current()
        GlobalScope.launch {
            while (leftTime > 0) {
                leftTime -= (current() - startTime)
                delay(500)
            }
            notify()
        }
    }

    fun getLeftTime(): Long = if ((this.leftTime) < 0) 0 else this.leftTime

    private fun current() = Calendar.getInstance().timeInMillis
}

