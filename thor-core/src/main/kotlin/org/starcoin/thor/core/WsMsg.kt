package org.starcoin.thor.core

import com.google.common.base.Preconditions
import kotlinx.serialization.*
import kotlinx.serialization.context.getOrDefault
import kotlinx.serialization.json.Json
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
    JOIN_ROOM(8),

    PAYMENT_REQ(10),
    PAYMENT_RESP(11),
    PAYMENT_START_REQ(12), PAYMENT_START_RESP(13),
    GAME_BEGIN(14),
    SURRENDER_REQ(14), SURRENDER_RESP(16),
    CHALLENGE_REQ(17),
    ROOM_DATA_MSG(99),
    UNKNOWN(100);
}

@UseExperimental(ImplicitReflectionSerializer::class)
abstract class Data {
    fun data2Str(): String {
        return Json.stringify(Json.plain.context.getOrDefault(this::class) as KSerializer<Data>, this)
    }
}

@Serializable
class ConfirmReq : Data()

@Serializable
data class ConfirmResp(val paymentRequest: String) : Data()

@Serializable
data class ConfirmPaymentReq(val paymentHash: String) : Data()

@Serializable
data class PaymentReq(val roomId: String, val rhash: String, val cost: Long) : Data()

@Serializable
data class PaymentResp(val roomId: String, val paymentRequest: String) : Data()

@Serializable
data class PaymentAndStartReq(val roomId: String, val paymentHash: String) : Data()

@Serializable
data class PaymentAndStartResp(val roomId: String) : Data()

@Serializable
data class BeginMsg2(val room: Room) : Data()

@Serializable
data class SurrenderReq(val roomId: String) : Data()

@Serializable
data class SurrenderResp(val r: String) : Data()

@Serializable
data class ChallengeReq(val instanceId: String) : Data()

@Serializable
data class JoinRoomReq(val roomId: String) : Data()

@Serializable
data class SessionId(val id: String) : Data()

@Serializable
@UseExperimental(ImplicitReflectionSerializer::class)
data class WsMsg(val type: MsgType, val data: String, var from: String? = null, var to: String? = null) {

    companion object {
        fun str2WsMsg(msg: String): WsMsg {
            return Json.parse(this.serializer(), msg)
        }
    }

    fun <T : Data> str2Data(cls: KClass<T>): T {
        return Json.parse(cls.serializer(), this.data)
    }

    fun msg2Str(): String {
        return Json.stringify(this)
    }
}

enum class HttpType(private val type: Int) {
    DEF(0), CREATE_GAME(1), GAME_LIST(2), CREATE_ROOM(3), ROOM_LIST(4), ROOM(5), ERR(100);
}

@Serializable
@UseExperimental(ImplicitReflectionSerializer::class)
data class HttpMsg(val type: HttpType, val data: String) {
    fun <T : Data> str2Data(cls: KClass<T>): T {
        return Json.parse(cls.serializer(), this.data)
    }

    fun toJson(): String {
        return Json.stringify(this)
    }
}

@Serializable
data class CreateGameReq(val gameHash: String) : Data()

@Serializable
data class GameListReq(val page: Int) : Data()

@Serializable
data class GameListResp(val count: Int, val data: List<GameInfo>?) : Data()

@Serializable
data class CreateRoomReq(val gameHash: String, val deposit: Long) : Data()

@Serializable
data class GetRoomReq(val roomId: String) : Data()

@Serializable
data class CreateRoomResp(val roomId: String?) : Data()

@Serializable
data class RoomListReq(val gameHash: String) : Data()

@Serializable
data class RoomListResp(val data: List<Room>?) : Data()

@Serializable
data class GameInfo(val addr: String, val name: String, val gameHash: String)

@Serializable
data class Room(val id: String, val gameHash: String, val players: MutableList<String>, val capacity: Int, val payment: Boolean = false, val cost: Long = 0, var payments: MutableList<String>? = null) {

    @kotlinx.serialization.Transient
    val isFull: Boolean
        get() = !this.players.isNullOrEmpty() && this.players.size >= capacity

    @kotlinx.serialization.Transient
    val isFullPayment: Boolean
        get() = !this.payments.isNullOrEmpty() && this.payments!!.size >= capacity

    constructor(gameHash: String) : this(randomString(), gameHash, mutableListOf(), 2)
    constructor(gameHash: String, cost: Long) : this(randomString(), gameHash, mutableListOf(), 2, true, cost, mutableListOf()) {
        Preconditions.checkArgument(cost > 0)
    }
}