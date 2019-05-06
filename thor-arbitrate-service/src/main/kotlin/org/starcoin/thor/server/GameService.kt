package org.starcoin.thor.server

import org.starcoin.thor.core.*

interface GameService {
    fun createGame(req: CreateGameReq): CreateGameResp

    fun gameList(page: Int): GameListResp

    fun roomList(page: Int): List<Room>

    fun roomList(game: String): List<Room>?

    fun queryRoom(roomId: String): Room

    fun queryGame(gameId: String): GameInfo?
}