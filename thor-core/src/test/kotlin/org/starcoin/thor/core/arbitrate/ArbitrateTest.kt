package org.starcoin.thor.core.arbitrate

import org.junit.Assert
import org.junit.Test
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.WitnessData
import java.io.FileReader
import java.util.*
import kotlin.concurrent.thread

class MockContractInput(private val userId: String) : ContractInput {
    override fun reset() {
        it = proof.iterator()
    }

    private val proof = ArrayList<ArbitrateDataImpl>()
    private var it: Iterator<ArbitrateDataImpl>

    init {
        proof.add(ArbitrateDataImpl(
                WitnessData(this.userId,
                        ByteArrayWrapper(ByteArray(1)), "",
                        ByteArrayWrapper(ByteArray(1)),1,null,null)
                ))
        it = proof.iterator()

    }

    override fun next(): ArbitrateData {

        return it.next()
    }

    override fun hasNext(): Boolean {
        return it.hasNext()
    }

    override fun getUser(): String {
        return this.userId
    }

}

class ArbitrateTest {
    @Test
    fun testChallenge() {
        val arb = ArbitrateImpl(2000) { it -> println("the winner:$it") }
        val proof = MockContractInput("1")
        val proof1 = MockContractInput("2")

        val srcCode = this::class.java.getResource("/engine.wasm").readBytes()
        Assert.assertTrue(arb.join("1", ContractImpl("http://localhost:3000", "1", srcCode)))
        Assert.assertTrue(arb.join("2", ContractImpl("http://localhost:3000", "2", srcCode)))
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