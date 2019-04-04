package org.starcoin.thor.server

import org.junit.Assert
import org.junit.Test
import org.starcoin.lightning.client.HashUtils
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import org.starcoin.lightning.client.core.Invoice
import org.starcoin.lightning.client.core.Payment

class ArbiterTest {
    @Test
    fun testArbitrate() {

        val ch1 = Utils.buildChannel(
                this.javaClass.classLoader.getResourceAsStream("alice.cert"),
                "starcoin-firstbox",
                30009
        )
        val alice = Account(ch1, 50)

        val ch2 = Utils.buildChannel(
                this.javaClass.classLoader.getResourceAsStream("bob.cert"),
                "starcoin-firstbox",
                40009
        )
        val bob = Account(ch2, 100)

        val arbiter = Arbiter()
        arbiter.register(alice)
        arbiter.register(bob)

        arbiter.match(alice, bob)

        val preimage = arbiter.challenge(alice)
        if (preimage != null) {
            Assert.assertEquals(alice.preimage, preimage)
        }

    }


    @Test
    fun testAddInvoice() {
        val cert = this.javaClass.classLoader.getResourceAsStream("alice.cert")
        val channel = Utils.buildChannel(cert, "starcoin-firstbox", 30009)
        val client = SyncClient(channel)
        val value = "abc"
        val invoice = Invoice(HashUtils.hash160(value.toByteArray()), 20)

        val addInvoiceResponse = client.addInvoice(invoice)

        val cert1 = this.javaClass.classLoader.getResourceAsStream("bob.cert")
        val channel1 = Utils.buildChannel(cert1, "starcoin-firstbox", 40009)
        val client1 = SyncClient(channel1)
        println(client1.sendPayment(Payment(addInvoiceResponse.getPaymentRequest())))

    }
}