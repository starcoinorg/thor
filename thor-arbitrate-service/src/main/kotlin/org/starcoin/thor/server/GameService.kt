package org.starcoin.thor.server

import org.starcoin.thor.core.CreateGameReq
import org.starcoin.thor.core.CreateGameResp
import org.starcoin.thor.core.GameListResp

interface GameService {
    fun createGame(req: CreateGameReq): CreateGameResp

    fun gameList(page: Int): GameListResp
}