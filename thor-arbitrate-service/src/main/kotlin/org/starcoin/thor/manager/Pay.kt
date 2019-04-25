package org.starcoin.thor.manager

import org.starcoin.lightning.client.HashUtils
import org.starcoin.thor.utils.encodeToBase58String
import org.starcoin.thor.utils.randomString

data class PaymentInfo(val userId: String, val r: String, val rHash: String)

class PaymentManager {
    private val paymentMap = mutableMapOf<String, Pair<PaymentInfo, PaymentInfo>>()//roomId -> userId

    fun generatePayments(roomId: String, firstUserId: String, secondUserId: String): Pair<PaymentInfo, PaymentInfo> {
        return when (paymentMap.containsKey(roomId)) {
            false -> {
                val first = generatePaymentInfo(firstUserId)
                val second = generatePaymentInfo(secondUserId)

                val newPair = Pair(first, second)
                synchronized(this) {
                    paymentMap[roomId] = newPair
                }
                newPair
            }
            true -> paymentMap[roomId]!!
        }
    }

    fun queryPlayer(currentUserId: String, roomId: String): String? {
        val pair = paymentMap[roomId]
        return when (currentUserId) {
            pair!!.first.userId -> pair.second.userId
            pair.second.userId -> pair.first.userId
            else -> null
        }
    }

    fun surrenderR(surrender: String, roomId: String): String? {
        val pair = paymentMap[roomId]
        return when (surrender) {
            pair!!.first.userId -> pair.second.r
            pair.second.userId -> pair.first.r
            else -> null
        }
    }

    fun queryRHash(userId: String, roomId: String): String? {
        val pair = paymentMap[roomId]
        return when (userId) {
            pair!!.first.userId -> pair.first.rHash
            pair.second.userId -> pair.second.rHash
            else -> null
        }
    }

    private fun generatePaymentInfo(addr: String): PaymentInfo {
        val r = randomString()
        val rHash = HashUtils.hash160(r.toByteArray()).encodeToBase58String()
        return PaymentInfo(addr, r, rHash)
    }

    fun clearPaymentInfoByRoomId(roomId: String) {
        paymentMap.remove(roomId)
    }
}