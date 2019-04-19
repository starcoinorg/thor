package org.starcoin.thor.core.arbitrate

class Arbitrate(val runtime: Runtime, val period: Int) {
    private var winner = 0
    private var status: Status = Status.NOTOPEN

    fun challenge(userId: Int, proof: ContractInput) {
        //TODO: Check time period
        this.winner = runtime.excute(proof)
        if (userId != this.winner) {
            this.status = Status.FINISH
        }
    }
    fun getStatus(){
        
    }
}


enum class Status {
    NOTOPEN,
    OPEN,
    FINISH,
}
