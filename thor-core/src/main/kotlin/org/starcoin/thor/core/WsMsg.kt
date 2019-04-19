package org.starcoin.thor.core

import kotlinx.serialization.*
import org.starcoin.sirius.serialization.ByteArrayWrapper

enum class MsgType {
    NONCE,
    CREATE_ROOM_REQ,
    CREATE_ROOM_RESP,
    JOIN_ROOM,
    PAYMENT_REQ,
    PAYMENT_RESP,
    PAYMENT_START_REQ, PAYMENT_START_RESP,
    GAME_BEGIN,
    SURRENDER_REQ, SURRENDER_RESP,
    CHALLENGE_REQ,
    ROOM_DATA_MSG,
    FORWARD_MSG,
    UNKNOWN;
}

@Serializable
class Nonce(@SerialId(1) val nonce: Long, @SerialId(2) val pubKey: ByteArrayWrapper) : Data()

@Serializable
data class CreateRoomReq(val gameHash: String, val deposit: Long = 0, val time: Long = 0) : Data()

@Serializable
data class CreateRoomResp(val roomId: String?) : Data()

@Serializable
data class JoinRoomReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class PaymentReq(@SerialId(1) val roomId: String, @SerialId(2) val rhash: String, @SerialId(3) val cost: Long) : Data()

@Serializable
data class PaymentResp(@SerialId(1) val roomId: String, @SerialId(2) val paymentRequest: String) : Data()

@Serializable
data class PaymentAndStartReq(@SerialId(1) val roomId: String, @SerialId(2) val paymentHash: String) : Data()

@Serializable
data class PaymentAndStartResp(@SerialId(1) val roomId: String) : Data()

@Serializable
data class BeginMsg2(@SerialId(1) val room: Room) : Data()

@Serializable
data class SurrenderReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class SurrenderResp(@SerialId(1) val r: String) : Data()

@Serializable
data class ChallengeReq(@SerialId(1) val instanceId: String) : Data()

@Serializable
data class RoomData(@SerialId(1) val to: String, @SerialId(2) val data: ByteArrayWrapper) : Data()

@Serializable
data class WitnessData(@SerialId(1) val data: ByteArrayWrapper, @SerialId(2) val timestamp: Long, @SerialId(3) val arbitSign: String, @SerialId(4) val firstPlayerSign: String, @SerialId(5) val secondPlayerSign: String)

@Serializable
data class ForwardMsg(@SerialId(1) val timestamp: Long, @SerialId(2) val msg: SignMsg) : Data()

@Serializable
data class WsMsg(@SerialId(1) val type: MsgType, @SerialId(2) var userId: String, @SerialId(3) val data: Data) : MsgObject() {
    companion object {
        fun str2WsMsg(msg: String): WsMsg {
            return fromJson(msg, WsMsg::class)
        }
    }

    fun msg2Str(): String {
        return toJson()
    }
}

@Serializable
data class SignMsg(@SerialId(1) val msg: WsMsg, @SerialId(2) val sign: String) : MsgObject()
