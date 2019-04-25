package org.starcoin.thor.core

import kotlinx.serialization.*
import org.starcoin.sirius.serialization.ByteArrayWrapper

enum class HttpType {
    DEF, PUB_KEY, CREATE_GAME, GAME_LIST, GAME_INFO, CREATE_ROOM, ROOM_LIST, ALL_ROOM_LIST, ROOM;
}

@Serializable
@UseExperimental(ImplicitReflectionSerializer::class)
data class HttpMsg(val type: HttpType, val data: Data) : MsgObject() {

    @Serializer(HttpMsg::class)
    companion object : KSerializer<HttpMsg> {
        private val desc = descriptor
        override fun deserialize(decoder: Decoder): HttpMsg {
            val cd = decoder.beginStructure(desc)
            val mt = cd.decodeStringElement(desc, cd.decodeElementIndex(desc))

            val index = cd.decodeElementIndex(desc)
            val data = when (mt) {
                HttpType.PUB_KEY.name -> HttpMsg(HttpType.PUB_KEY, cd.decodeSerializableElement(desc, index, PubKeyReq::class.serializer()))
                HttpType.CREATE_GAME.name -> HttpMsg(HttpType.CREATE_GAME, cd.decodeSerializableElement(desc, index, CreateGameReq::class.serializer()))
                HttpType.GAME_LIST.name -> HttpMsg(HttpType.GAME_LIST, cd.decodeSerializableElement(desc, index, GameListReq::class.serializer()))
                HttpType.GAME_INFO.name -> HttpMsg(HttpType.GAME_INFO, cd.decodeSerializableElement(desc, index, GameInfoReq::class.serializer()))
                HttpType.CREATE_ROOM.name -> HttpMsg(HttpType.CREATE_ROOM, cd.decodeSerializableElement(desc, index, CreateRoomReq::class.serializer()))
                HttpType.ROOM_LIST.name -> HttpMsg(HttpType.ROOM_LIST, cd.decodeSerializableElement(desc, index, RoomListByGameReq::class.serializer()))
                HttpType.ALL_ROOM_LIST.name -> HttpMsg(HttpType.ALL_ROOM_LIST, cd.decodeSerializableElement(desc, index, RoomListReq::class.serializer()))
                HttpType.ROOM.name -> HttpMsg(HttpType.ROOM, cd.decodeSerializableElement(desc, index, GetRoomReq::class.serializer()))
                else -> {
                    throw Exception("unknown http type")
                }
            }
            cd.endStructure(desc)
            return data
        }

        override fun serialize(encoder: Encoder, obj: HttpMsg) {
            val ce = encoder.beginStructure(desc)
            ce.encodeStringElement(desc, 0, obj.type.name)
            ce.encodeSerializableElement(desc, 1, obj.data.javaClass.kotlin.serializer(), obj.data)
            ce.endStructure(desc)
        }
    }
}

@Serializable
class PubKeyReq : Data()

@Serializable
data class PubKeyResp(val pubKey: ByteArrayWrapper?) : Data()

@Serializable
data class CreateGameReq(val gameName: String, val engine: ByteArrayWrapper, val gui: ByteArrayWrapper) : Data()

@Serializable
data class CreateGameResp(val game:GameBaseInfo?) : Data()

@Serializable
data class GameListReq(val page: Int) : Data()

@Serializable
data class GameListResp(val count: Int, val data: List<GameBaseInfo>?) : Data()

@Serializable
data class GameInfoReq(val gameId: String) : Data()

@Serializable
data class GetRoomReq(val roomId: String) : Data()

@Serializable
data class RoomListReq(val page: Int) : Data()

@Serializable
data class RoomListResp(val data: List<Room>?) : Data()

@Serializable
data class RoomListByGameReq(val gameId: String) : Data()

@Serializable
data class RoomListByGameResp(val data: List<Room>?) : Data()
