package org.starcoin.thor.manager

import org.starcoin.lightning.client.HashUtils
import org.starcoin.thor.utils.encodeToBase58String
import org.starcoin.thor.utils.randomString

data class PaymentInfo(val userId: String, val r: String, val rHash: String, var ready: Boolean = false)

class PaymentManager {
    private val paymentMap = mutableMapOf<String, Pair<PaymentInfo, PaymentInfo>>()

    fun generatePayments(roomId: String, firstSessionId: String, secondSessionId: String): Pair<PaymentInfo, PaymentInfo> {
        return when (paymentMap[roomId]) {
            null -> {
                val first = generatePaymentInfo(firstSessionId)
                val second = generatePaymentInfo(secondSessionId)

                val newPair = Pair(first, second)
                synchronized(this) {
                    paymentMap[roomId] = newPair
                }
                newPair
            }
            else -> paymentMap[roomId]!!
        }
    }

    fun queryPlayer(surrender: String, roomId: String): String? {
        val pair = paymentMap[roomId]
        return when (surrender) {
            pair!!.first.userId -> pair.second.userId
            pair.second.userId -> pair.first.userId
            else -> null
        }
    }

    fun userReady(userId: String, roomId: String) {
        val pair = paymentMap[roomId]
        return synchronized(this) {
            when (userId) {
                pair!!.first.userId -> pair.first.ready = true
                pair.second.userId -> pair.first.ready = true
                else -> null
            }
        }
    }

    fun roomReady(roomId: String): Boolean {
        paymentMap[roomId]?.let {
            return paymentMap[roomId]!!.first.ready && paymentMap[roomId]!!.second.ready
        }
        return false
    }

    fun surrenderR(surrender: String, instanceId: String): String? {
        val pair = paymentMap[instanceId]
        return when (surrender) {
            pair!!.first.userId -> pair.second.r
            pair.second.userId -> pair.first.r
            else -> null
        }
    }

    private fun generatePaymentInfo(addr: String): PaymentInfo {
        val r = randomString()
        val rHash = HashUtils.hash160(r.toByteArray()).encodeToBase58String()
        return PaymentInfo(addr, r, rHash)
    }
}