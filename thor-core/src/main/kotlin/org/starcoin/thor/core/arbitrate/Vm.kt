package org.starcoin.thor.core.arbitrate


interface Runtime {
    fun excute(input: ContractInput): Int
}

abstract class ContractInput {
    abstract fun getNext(): ByteArray?
}

abstract class Contract {
    abstract fun run(input: ByteArray)
    abstract fun getWinner(): Int
}

class Vm(val contract: Contract) : Runtime {

    override fun excute(input: ContractInput): Int {
        var inputArg = input.getNext()
        while (inputArg != null) {
            contract.run(inputArg)
        }
        return contract.getWinner()
    }

}