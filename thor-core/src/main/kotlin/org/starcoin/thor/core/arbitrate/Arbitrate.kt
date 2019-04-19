package org.starcoin.thor.core.arbitrate

import org.starcoin.thor.core.Room


abstract class Arbitrate {

    abstract fun initVm(contract: Contract, period: Int)

    abstract fun openChallenge(user: User, proof: Proof)

    abstract fun getChallengeResult(): ChallengeResult

}

abstract class ChallengeResult {
    abstract fun getStatus(): ChallengeStatus
    abstract fun getWinner(): User?
}

enum class ChallengeStatus {
    NOTOPEN,
    OPEN,
    FINISH,
}

abstract class User 
