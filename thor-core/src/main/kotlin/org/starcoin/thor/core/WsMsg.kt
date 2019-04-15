package org.starcoin.thor.core

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.base.Preconditions
import org.starcoin.thor.utils.randomString
import java.io.InputStream
import kotlin.reflect.KClass

data class LnConfig(val cert: InputStream, val host: String, val port: Int)

enum class MsgType(private val type: Int) {
    CONN(1),
    CONFIRM_REQ(2), CONFIRM_RESP(3),
    CONFIRM_PAYMENT_REQ(4), CONFIRM_PAYMENT_RESP(5),
    CREATE_ROOM_REQ(6),
    CREATE_ROOM_RESP(7),
    JOIN_ROOM_FREE(8),
    JOIN_ROOM_PAY(9),

    PAYMENT_REQ(10),
    PAYMENT_RESP(11),
    PAYMENT_START_REQ(12), PAYMENT_START_RESP(13),
    GAME_BEGIN(14),
    SURRENDER_REQ(14), SURRENDER_RESP(16),
    CHALLENGE_REQ(17),
    ROOM_DATA_MSG(99),
    UNKNOWN(100);

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromInt(t: Int): MsgType =
                values().firstOrNull { it.type == t } ?: MsgType.UNKNOWN
    }

    @JsonValue
    fun toInt(): Int {
        return type
    }
}

abstract class Data {
    fun data2Str(): String {
        return om.writeValueAsString(this)
    }
}

class ConfirmReq : Data()

data class ConfirmResp(val paymentRequest: String) : Data()

data class ConfirmPaymentReq(val paymentHash: String) : Data()

data class PaymentReq(val roomId: String, val rhash: String, val cost: Long) : Data()

data class PaymentResp(val roomId: String, val paymentRequest: String) : Data()

data class PaymentAndStartReq(val roomId: String, val paymentHash: String) : Data()

data class PaymentAndStartResp(val roomId: String) : Data()

data class BeginMsg(val instanceId: String) : Data()
data class BeginMsg2(val room: Room) : Data()

data class SurrenderReq(val roomId: String) : Data()

data class SurrenderResp(val r: String) : Data()

data class ChallengeReq(val instanceId: String) : Data()

data class JoinRoomReq(val roomId: String) : Data()

data class SessionId(val id: String) : Data()

data class WsMsg(val type: MsgType, val data: String, var from: String? = null, var to: String? = null) {
    companion object {
        fun str2WsMsg(msg: String): WsMsg {
            return om.readValue(msg, WsMsg::class.java)
        }
    }

    fun <T : Data> str2Data(cls: KClass<T>): T {
        return om.readValue(this.data, cls.java)
    }

    fun msg2Str(): String {
        return om.writeValueAsString(this)
    }
}

private val om = ObjectMapper().registerModule(KotlinModule())

enum class HttpType(private val type: Int) {
    DEF(0), CREATE_GAME(1), GAME_LIST(2), CREATE_ROOM(3), ROOM_LIST(4), ROOM(5), ERR(100);

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromInt(t: Int): HttpType =
                HttpType.values().firstOrNull { it.type == t } ?: HttpType.DEF
    }

    @JsonValue
    fun toInt(): Int {
        return type
    }
}

data class HttpMsg(val type: HttpType, val data: String) {
    companion object {
        fun str2HttpMsg(msg: String): HttpMsg {
            return om.readValue(msg, HttpMsg::class.java)
        }
    }

    fun <T : Data> str2Data(cls: KClass<T>): T {
        return om.readValue(this.data, cls.java)
    }

    fun msg2Str(): String {
        return om.writeValueAsString(this)
    }
}


data class CreateGameReq(val gameHash: String) : Data()

data class GameListReq(val page: Int) : Data()

data class GameListResp(val count: Int, val data: List<GameInfo>?) : Data()

data class CreateRoomReq(val gameHash: String, val deposit: Long) : Data()

data class GetRoomReq(val roomId: String) : Data()

data class CreateRoomResp(val roomId: String?) : Data()

data class RoomListReq(val gameHash: String) : Data()

data class RoomListResp(val data: List<Room>?) : Data()

data class GameInfo(val addr: String, val name: String, val gameHash: String)
data class Room(val id: String, val gameHash: String, val players: MutableList<String>, val capacity: Int, val payment: Boolean = false, val cost: Long = 0, var payments: MutableList<String>? = null) {

    val isFull: Boolean
        get() = this.players.size >= capacity

    val isFullPayment: Boolean
        get() = this.payments!!.size >= capacity

    constructor(gameHash: String) : this(randomString(), gameHash, mutableListOf(), 2)
    constructor(gameHash: String, cost: Long) : this(randomString(), gameHash, mutableListOf(), 2, true, cost, mutableListOf()) {
        Preconditions.checkArgument(cost > 0)
    }
}