package org.starcoin.thor.core

import kotlinx.serialization.*
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.sign.SignService
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.PublicKey

enum class MsgType {
    NONCE,
    CREATE_ROOM_REQ,
    CREATE_ROOM_RESP,
    JOIN_ROOM_REQ,
    JOIN_ROOM_RESP,
    HASH_DATA,
    INVOICE_DATA,
    READY_REQ,
    GAME_BEGIN,
    SURRENDER_REQ,
    SURRENDER_RESP,
    CHALLENGE_REQ,
    ROOM_GAME_DATA_MSG,
    ROOM_COMMON_DATA_MSG,
    GAME_END,
    UNKNOWN,
    ERR
}

@Serializable
class Nonce(@SerialId(1) val nonce: String, @SerialId(2) val pubKey: ByteArrayWrapper) : Data() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Nonce

        if (nonce != other.nonce) return false
        if (pubKey != other.pubKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nonce.hashCode()
        result = 31 * result + pubKey.hashCode()
        return result
    }
}

@Serializable
data class CreateRoomReq(@SerialId(1) val gameHash: String, @SerialId(2) val cost: Long = 0, @SerialId(3) val time: Long = 0) : Data()

@Serializable
data class CreateRoomResp(@SerialId(1) val room: Room?) : Data()

@Serializable
data class JoinRoomReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class JoinRoomResp(@SerialId(1) val succ: Boolean, @SerialId(2) val room: Room? = null) : Data()

@Serializable
data class HashData(@SerialId(1) val roomId: String, @SerialId(2) val rhash: ByteArrayWrapper, @SerialId(3) val cost: Long) : Data()

@Serializable
data class InvoiceData(@SerialId(1) val roomId: String, @SerialId(2) val paymentRequest: String) : Data()

@Serializable
data class ReadyReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class BeginMsg(@SerialId(1) val room: Room, @SerialId(2) var timestamp: Long, @SerialId(3) var keys: List<ByteArrayWrapper>? = null) : Data()

@Serializable
data class SurrenderReq(@SerialId(1) val roomId: String) : Data()

@Serializable
data class SurrenderResp(@SerialId(1) val r: ByteArrayWrapper) : Data()

@Serializable
data class ChallengeReq(@SerialId(1) val roomId: String, @SerialId(2) val witnessList: List<WitnessData>) : Data()

@Serializable
data class RoomGameData(@SerialId(1) val to: String, @SerialId(2) val witness: WitnessData) : Data()

@Serializable
data class CommonRoomData(@SerialId(1) val to: String, @SerialId(2) val data: String) : Data()

@Serializable
data class GameEnd(@SerialId(1) val roomId: String) : Data()

@Serializable
data class ErrMsg(@SerialId(1) val code: Int, @SerialId(2) val err: String) : Data()

@Serializable
data class WitnessData(@SerialId(1) var userId: String, @SerialId(2) var stateHash: ByteArrayWrapper, @SerialId(3) var preSign: String, @SerialId(4) val data: ByteArrayWrapper, @SerialId(5) var timestamp: Long? = null, @SerialId(6) var arbiterSign: String? = null, @SerialId(7) var sign: String? = null) {

    fun doSign(privateKey: PrivateKey) {
        val signData = userId.toByteArray() + stateHash.bytes + data.bytes
        sign = SignService.sign(signData, privateKey)
    }

    fun doArbiterSign(privateKey: PrivateKey) {
        timestamp = System.currentTimeMillis()
        val signData = userId.toByteArray() + stateHash.bytes + data.bytes + longToBytes(timestamp!!)
        arbiterSign = SignService.sign(signData, privateKey)
    }

    fun checkArbiterSign(publicKey: PublicKey): Boolean {
        val signData = userId.toByteArray() + stateHash.bytes + data.bytes + longToBytes(timestamp!!)
        return SignService.verifySign(signData, arbiterSign!!, publicKey)
    }


    fun checkSign(publicKey: PublicKey): Boolean {
        if (sign != null) {
            val signData = userId.toByteArray() + stateHash.bytes + data.bytes
            return SignService.verifySign(signData, sign!!, publicKey)
        }
        return false
    }
}

@Serializable
@UseExperimental(ImplicitReflectionSerializer::class)
data class WsMsg(@SerialId(1) val type: MsgType, @SerialId(2) var userId: String, @SerialId(3) var data: Data) : MsgObject() {

    @Serializer(WsMsg::class)
    companion object : KSerializer<WsMsg> {
        private val desc = descriptor
        override fun deserialize(decoder: Decoder): WsMsg {
            val cd = decoder.beginStructure(desc)
            val mt = cd.decodeStringElement(desc, cd.decodeElementIndex(desc))
            val userId = cd.decodeStringElement(desc, cd.decodeElementIndex(desc))

            val index = cd.decodeElementIndex(desc)
            val data = when (mt) {
                MsgType.NONCE.name -> WsMsg(MsgType.NONCE, userId, cd.decodeSerializableElement(desc, index, Nonce::class.serializer()))
                MsgType.CREATE_ROOM_REQ.name -> WsMsg(MsgType.CREATE_ROOM_REQ, userId, cd.decodeSerializableElement(desc, index, CreateRoomReq::class.serializer()))
                MsgType.CREATE_ROOM_RESP.name -> WsMsg(MsgType.CREATE_ROOM_RESP, userId, cd.decodeSerializableElement(desc, index, CreateRoomResp::class.serializer()))
                MsgType.JOIN_ROOM_REQ.name -> WsMsg(MsgType.JOIN_ROOM_REQ, userId, cd.decodeSerializableElement(desc, index, JoinRoomReq::class.serializer()))
                MsgType.JOIN_ROOM_RESP.name -> WsMsg(MsgType.JOIN_ROOM_RESP, userId, cd.decodeSerializableElement(desc, index, JoinRoomResp::class.serializer()))
                MsgType.HASH_DATA.name -> WsMsg(MsgType.HASH_DATA, userId, cd.decodeSerializableElement(desc, index, HashData::class.serializer()))
                MsgType.INVOICE_DATA.name -> WsMsg(MsgType.INVOICE_DATA, userId, cd.decodeSerializableElement(desc, index, InvoiceData::class.serializer()))
                MsgType.READY_REQ.name -> WsMsg(MsgType.READY_REQ, userId, cd.decodeSerializableElement(desc, index, ReadyReq::class.serializer()))
                MsgType.GAME_BEGIN.name -> WsMsg(MsgType.GAME_BEGIN, userId, cd.decodeSerializableElement(desc, index, BeginMsg::class.serializer()))
                MsgType.SURRENDER_REQ.name -> WsMsg(MsgType.SURRENDER_REQ, userId, cd.decodeSerializableElement(desc, index, SurrenderReq::class.serializer()))
                MsgType.SURRENDER_RESP.name -> WsMsg(MsgType.SURRENDER_RESP, userId, cd.decodeSerializableElement(desc, index, SurrenderResp::class.serializer()))
                MsgType.CHALLENGE_REQ.name -> WsMsg(MsgType.CHALLENGE_REQ, userId, cd.decodeSerializableElement(desc, index, ChallengeReq::class.serializer()))
                MsgType.ROOM_GAME_DATA_MSG.name -> WsMsg(MsgType.ROOM_GAME_DATA_MSG, userId, cd.decodeSerializableElement(desc, index, RoomGameData::class.serializer()))
                MsgType.ROOM_COMMON_DATA_MSG.name -> WsMsg(MsgType.ROOM_COMMON_DATA_MSG, userId, cd.decodeSerializableElement(desc, index, CommonRoomData::class.serializer()))
                else -> {
                    throw Exception()
                }
            }
            cd.endStructure(desc)
            return data
        }

        override fun serialize(encoder: Encoder, obj: WsMsg) {
            val ce = encoder.beginStructure(desc)
            ce.encodeStringElement(desc, 0, obj.type.name)
            ce.encodeStringElement(desc, 1, obj.userId)
            ce.encodeSerializableElement(desc, 2, obj.data.javaClass.kotlin.serializer(), obj.data)
            ce.endStructure(desc)
        }
    }
}

@Serializable
data class SignMsg(@SerialId(1) val msg: WsMsg, @SerialId(2) val sign: String) : MsgObject()

fun longToBytes(l: Long): ByteArray {
    val buffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    buffer.putLong(l)
    return buffer.array()
}