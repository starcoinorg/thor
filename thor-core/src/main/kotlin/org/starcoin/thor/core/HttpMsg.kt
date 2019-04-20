package org.starcoin.thor.core

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.stringify
import org.starcoin.sirius.serialization.ByteArrayWrapper

enum class HttpType {
    DEF, PUB_KEY, CREATE_GAME, GAME_LIST, CREATE_ROOM, ROOM_LIST,ALL_ROOM_LIST, ROOM, ERR;
}

@Serializable
@UseExperimental(ImplicitReflectionSerializer::class)
data class HttpMsg(val type: HttpType, val data: Data) {
    fun toJson(): String {
        return Json.stringify(this)
    }
}

@Serializable
class PubKeyReq : Data()

@Serializable
data class PubKeyResp(val pubKey: ByteArrayWrapper?) : Data()

@Serializable
data class CreateGameReq(val gameName: String, val game: ByteArrayWrapper, val count: Int) : Data()

@Serializable
data class CreateGameResp(val succ: Boolean) : Data()

@Serializable
data class GameListReq(val page: Int) : Data()

@Serializable
data class GameListResp(val count: Int, val data: List<GameBaseInfo>?) : Data()

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