package org.starcoin.thor.server

import org.starcoin.thor.core.*
import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager

class GameServiceImpl(private val gameManager: GameManager, private val roomManager: RoomManager) : GameService {
    private val size = 10

    override fun createGame(req: CreateGameReq): CreateGameResp {
        val gameInfo = GameInfo(req.gameName, req.engine, req.gui)
        return CreateGameResp(gameManager.createGame(gameInfo))
    }

    override fun gameList(page: Int): GameListResp {
        val begin = when (page <= 0) {
            true -> 0
            false -> (page - 1) * size
        }

        val count = gameManager.count()
        val end = when (begin + size < count) {
            true -> begin + size
            false -> count
        }

        val data = gameManager.list(begin, end)
        return GameListResp(count, data)
    }

    override fun roomList(page: Int): List<Room> {
        val begin = when (page <= 0) {
            true -> 0
            false -> (page - 1) * size
        }

        val count = roomManager.count()
        val end = when (begin + size < count) {
            true -> begin + size
            false -> count
        }

        return roomManager.queryRoomList(begin, end)
    }

    override fun roomList(game: String): List<Room>? {
        return roomManager.queryRoomListByGame(game)
    }

    override fun queryRoom(roomId: String): Room {
        return roomManager.queryRoomNotNull(roomId)
    }

    override fun queryGame(gameId: String): GameInfo {
        return gameManager.queryGameInfoByHash(gameId)
    }
}