package org.starcoin.thor.server

import org.starcoin.thor.core.CreateGameReq
import org.starcoin.thor.core.CreateGameResp
import org.starcoin.thor.core.GameListResp
import org.starcoin.thor.core.Room

interface GameService {
    fun createGame(req: CreateGameReq): CreateGameResp

    fun gameList(page: Int): GameListResp

    fun doCreateRoom(game: String, deposit: Long): Room

    fun doRoomList(page: Int): List<Room>

    fun doRoomList(game: String): List<Room>?

    fun getRoom(roomId: String): Room
}