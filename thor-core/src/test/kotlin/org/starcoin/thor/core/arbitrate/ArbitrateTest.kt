package org.starcoin.thor.core.arbitrate

import org.junit.Test

class MockContractInput(private val userId: Int) : ContractInput {
    private val proof = ArrayList<ByteArray>()
    val iter = proof.iterator()

    init {
        proof.add("0".toByteArray())
        proof.add("1".toByteArray())
        proof.add("1".toByteArray())
    }

    override fun next(): ByteArray {
        return iter.next()
    }

    override fun hasNext(): Boolean {
        return iter.hasNext()
    }

    override fun getUser(): Int {
        return this.userId
    }

}

class ArbitrateTest {
    @Test
    fun testChallenge() {
        val arb = Arbitrate(ContractImpl("http://localhost:3000", 1), 2000)
        val proof = MockContractInput(0)
        arb.challenge(0, proof)
        println(arb.getWinner())
    }
}