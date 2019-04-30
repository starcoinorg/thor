package org.starcoin.thor.manager

import io.ktor.features.NotFoundException
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.GameBaseInfo
import org.starcoin.thor.core.PlayerInfo
import org.starcoin.thor.core.Room
import org.starcoin.thor.core.UserInfo
import org.starcoin.thor.sign.toByteArray

//all userId
class RoomManager {
    private val rooms = mutableMapOf<String, Room>()
    private val roomLock = Object()

    private val roomSet = mutableSetOf<String>()
    private val game2Room = mutableMapOf<String, ArrayList<String>>()
    private val joinLock = Object()

    fun createRoom(game: GameBaseInfo, cost: Long, time: Long, userInfo: UserInfo? = null): Room {
        val room = Room(game.hash, cost, time)
        userInfo?.let { room.addPlayer(userInfo) }
        synchronized(roomLock) {
            roomSet.add(room.roomId)
            when (game2Room[game.hash]) {
                null -> {
                    val list = ArrayList<String>()
                    list.add(room.roomId)
                    game2Room[game.hash] = list
                }
                else -> {
                    game2Room[game.hash]!!.add(room.roomId)
                }
            }
            rooms[room.roomId] = room
        }
        return room
    }

    fun queryRoomListByGame(gameId: String): List<Room>? {
        val roomIds = game2Room[gameId]
        roomIds?.let { return rooms.filterKeys { roomIds.contains(it) }.values.toList() }
        return null
    }

    fun queryUserIndex(roomId: String, userId: String): Int {
        return rooms[roomId]!!.players.map { playerInfo -> playerInfo.playerUserId }.indexOf(userId) + 1
    }

    fun queryUserIdByIndex(roomId: String, userIndex: Int): String {
        return rooms[roomId]!!.players.map { playerInfo -> playerInfo.playerUserId }[userIndex]
    }

    fun queryRoomList(begin: Int, end: Int): List<Room> {
        val keys = roomSet.toList().subList(begin, end).toSet()
        return rooms.filterKeys { keys.contains(it) }.values.toList()
    }

    fun count(): Int {
        return this.rooms.size
    }

    private fun queryRoomOrNull(roomId: String): Room? {
        return rooms[roomId]
    }

    fun queryRoomNotNull(roomId: String): Room {
        return this.queryRoomOrNull(roomId) ?: throw NotFoundException("Can not find room by id $roomId")
    }

    fun roomBegin(roomId: String, time: Long) {
        rooms[roomId]!!.begin = time
    }

    fun clearRoom(roomId: String) {
        synchronized(roomLock) {
            roomSet.remove(roomId)
            val r = rooms[roomId]
            r?.let {
                game2Room[r.gameId]!!.remove(roomId)
                rooms.remove(roomId)
            }
        }
    }

    fun rHash(roomId: String, userId: String, rHash: ByteArrayWrapper) {
        queryRoomNotNull(roomId).rHash(userId, rHash)
    }

    fun joinRoom(userInfo: UserInfo, roomId: String): Room {
        return queryRoomNotNull(roomId).let {
            synchronized(joinLock) {
                if (it.isFull) {
                    throw RuntimeException("room $roomId is full.")
                }
                if (!it.isInRoom(userInfo.id)) {
                    it.players.add(PlayerInfo(userInfo.id, ByteArrayWrapper(userInfo.publicKey.toByteArray())))
                }
                it
            }
        }
    }
}