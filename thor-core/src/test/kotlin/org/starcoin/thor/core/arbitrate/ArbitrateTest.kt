package org.starcoin.thor.core.arbitrate

import org.junit.Assert
import org.junit.Test
import java.util.*
import kotlin.concurrent.thread

class MockContractInput(private val userId: Int) : ContractInput {
    private val proof = ArrayList<ByteArray>()
    private val it:Iterator<ByteArray>

    init {
        proof.add("0".toByteArray())
        proof.add("1".toByteArray())
        proof.add("1".toByteArray())
        it = proof.iterator()
    }

    override fun next(): ByteArray {

        return it.next()
    }

    override fun hasNext(): Boolean {
        return it.hasNext()
    }

    override fun getUser(): Int {
        return this.userId
    }

}

class ArbitrateTest {
    @Test
    fun testChallenge() {
        val arb = ArbitrateImpl(2000) { it -> println("the winner:$it") }
        val proof = MockContractInput(1)
        val proof1 = MockContractInput(2)
        Assert.assertTrue(arb.join(1, ContractImpl("http://localhost:3000", "1")))
        Assert.assertTrue(arb.join(2, ContractImpl("http://localhost:3000", "1")))
        arb.challenge(proof)
        arb.challenge(proof1)
        Thread.sleep(3000)
    }

    @Test
    fun testTimer() {
        fun current() = Calendar.getInstance().timeInMillis
        val t = Timer(2000) { println("notify run on:${current()}") }

        t.start()
        while (t.getLeftTime() != 0.toLong()) {
            Thread.sleep(1000)
            println("timer knock knock,${current()}")
        }
        Thread.sleep(1000)

    }
}