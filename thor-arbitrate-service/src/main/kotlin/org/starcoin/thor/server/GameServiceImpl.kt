package org.starcoin.thor.server

import org.starcoin.thor.core.CreateGameReq
import org.starcoin.thor.core.CreateGameResp
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.core.GameListResp
import org.starcoin.thor.manager.GameManager

class GameServiceImpl(private val gameManager: GameManager) : GameService {
    private val size = 10

    override fun createGame(req: CreateGameReq): CreateGameResp {
        val gameInfo = GameInfo(req.gameName, req.game, req.count)
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

//    fun doCreateRoom(game: String, deposit: Long): Room {
//        Preconditions.checkArgument(deposit >= 0)
//        val gameInfo = gameManager.queryGameByHash(game) ?: throw NotFoundException("Can not find game by hash: $game")
//        return roomManager.createRoom(gameInfo, deposit)
//    }
//
//    fun doRoomList(): List<Room> {
//        return roomManager.queryAllRoomList()
//    }
//
//    fun getRoom(roomId: String): Room {
//        return roomManager.getRoom(roomId)
//    }
}