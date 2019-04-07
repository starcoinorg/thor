package org.starcoin.thor.server

import io.ktor.http.cio.websocket.DefaultWebSocketSession
import org.starcoin.lightning.client.HashUtils
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.proto.Thor
import java.util.*

enum class UserStatus {
    UNKNOWN, CONNECTED, CONFIRMED, GAME_PAIR, GAME_FUNDED, GAME_ING
}

data class User(val session: DefaultWebSocketSession, var addr: String? = null, var stat: UserStatus = UserStatus.UNKNOWN)

data class PaymentInfo(val addr: String, val r: String, val rHash: ByteArray)

class UserManager {
    private val connections = mutableMapOf<String?, User>()

    fun addUser(user: User): Boolean {
        synchronized(this) {
            if (!connections.containsKey(user.addr)) {
                connections[user.addr] = user
                return true
            }
        }

        return false
    }

    fun queryUser(addr: String?): User? {
        return connections[addr]
    }
}

class PaymentManager {
    private val paymentMap = mutableMapOf<String, Pair<PaymentInfo, PaymentInfo>>()

    fun generatePayments(id: String, fromAddr: String, toAddr: String): Pair<PaymentInfo, PaymentInfo> {
        return when (paymentMap[id]) {
            null -> {
                val first = generatePaymentInfo(fromAddr)
                val second = generatePaymentInfo(toAddr)

                val newPair = Pair(first, second)
                synchronized(this) {
                    paymentMap[id] = newPair
                }
                newPair
            }
            else -> paymentMap[id]!!
        }
    }

    fun surrenderR(surrenderAddr: String, instanceId: String): Pair<String, String>? {
        val pair = paymentMap[instanceId]
        return when (surrenderAddr) {
            pair!!.first.addr -> Pair(pair.second.r, pair.second.addr)
            pair.second.addr -> Pair(pair.first.r, pair.first.addr)
            else -> null
        }
    }

    private fun generatePaymentInfo(addr: String): PaymentInfo {
        val r = randomString()
        val rHash = HashUtils.hash160(r.toByteArray())
        return PaymentInfo(addr, r, rHash)
    }
}

class GameManager(val adminAddr: String) {
    private val appMap = mutableMapOf<String, GameInfo>()
    private val nameSet = mutableSetOf<String>()
    private val gameHashSet = mutableSetOf<String>()
    private val count: Int = 0
    private val gameLock = java.lang.Object()
    private val instanceLock = java.lang.Object()

    private val gameInstanceMap = mutableMapOf<String, String>()

    fun createGame(game: GameInfo): Boolean {
        var flag = false
        synchronized(gameLock) {
            when (!nameSet.contains(game.name) && !gameHashSet.contains(game.gameHash.bytes.toHEXString())) {
                true -> {
                    nameSet.add(game.name)
                    gameHashSet.add(game.gameHash.bytes.toHEXString())
                    appMap[game.gameHash.bytes.toHEXString()] = game
                    count.inc()
                    flag = true
                }
            }
        }

        return flag
    }

    fun count(): Int {
        return this.count
    }

    fun list(begin: Int, end: Int): List<Thor.ProtoGameInfo> {
        var keys = gameHashSet.toList().subList(begin, end).toSet()
        return appMap.filterKeys { keys.contains(it) }.values.map { it.toProto<Thor.ProtoGameInfo>() }
    }

    fun queryGameByHash(hash: String): GameInfo? {
//        return appMap[hash]//TODO()
        return GameInfo.mock()
    }

    fun generateInstance(hash: String): String {
        val id = randomString()

        synchronized(instanceLock) {
            gameInstanceMap[id] = hash
        }

        return id
    }
}

fun randomString(): String {
    return UUID.randomUUID().toString().replace("-", "")
}
