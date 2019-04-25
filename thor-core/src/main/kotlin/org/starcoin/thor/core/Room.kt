package org.starcoin.thor.core

import com.google.common.base.Preconditions
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.sign.toByteArray
import org.starcoin.thor.utils.randomString

@Serializable
data class PlayerInfo(@SerialId(1) val playerUserId: String, @SerialId(2) val playerPubKey: ByteArrayWrapper, @SerialId(3) var ready: Boolean = false)

@Serializable
data class Room(@SerialId(1) val roomId: String, @SerialId(2) val gameId: String, @SerialId(3) var players: MutableList<PlayerInfo>, @SerialId(4) val capacity: Int, @SerialId(6) val cost: Long = 0, @SerialId(7) val time: Long = 0, @SerialId(8) var begin: Long = 0) : MsgObject() {

    @kotlinx.serialization.Transient
    val isFull: Boolean
        get() = !this.players.isNullOrEmpty() && this.players.size >= capacity

    @kotlinx.serialization.Transient
    val payment: Boolean
        get() = cost > 0

    constructor(gameId: String) : this(randomString(), gameId, mutableListOf(), 2)
    constructor(gameId: String, cost: Long) : this(randomString(), gameId, mutableListOf(), 2, cost, 0) {
        Preconditions.checkArgument(cost >= 0)
    }

    constructor(gameId: String, cost: Long, time: Long) : this(randomString(), gameId, mutableListOf(), 2, cost, time) {
        Preconditions.checkArgument(cost >= 0)
    }

    fun addPlayer(userInfo: UserInfo) {
        synchronized(this) {
            check(!isFull)
            if (!this.players.map { playerInfo -> playerInfo.playerUserId }.contains(userInfo.id)) {
                this.players.add(PlayerInfo(userInfo.id, ByteArrayWrapper(userInfo.publicKey.toByteArray())))
            }
        }
    }

    fun userReady(userId: String) {
        synchronized(this) {
            this.players.filter { playerInfo -> playerInfo.playerUserId == userId }[0].ready = true
        }
    }

    fun roomReady(): Boolean {
        var flag = true
        players.forEach { flag = (it.ready && flag) }
        return isFull && flag
    }
}