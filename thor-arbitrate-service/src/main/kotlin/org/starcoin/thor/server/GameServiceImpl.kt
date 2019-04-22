package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
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

    override fun doCreateRoom(game: String, deposit: Long): Room {
        Preconditions.checkArgument(deposit >= 0)
        val gameInfo = gameManager.queryGameBaseInfoByHash(game)
                ?: throw NotFoundException("Can not find game by hash: $game")
        return roomManager.createRoom(gameInfo, deposit, 0)
    }

    override fun doRoomList(page: Int): List<Room> {
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

    override fun doRoomList(game: String): List<Room>? {
        return roomManager.queryRoomListByGame(game)
    }

    override fun getRoom(roomId: String): Room {
        return roomManager.queryRoomNotNull(roomId)
    }
}