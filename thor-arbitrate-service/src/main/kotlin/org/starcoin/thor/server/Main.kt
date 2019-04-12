package org.starcoin.thor.server

import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.core.LnConfig
import java.util.*

fun main(args: Array<String>) {
    val arbiterCert = WebsocketServer::class.java.classLoader.getResourceAsStream("arb.cert")
    val arbiterConfig = LnConfig(arbiterCert, "starcoin-firstbox", 20009)

    val msgServer = WebsocketServer(arbiterConfig)
    msgServer.start()

    val testGame = "test-game-" + Random().nextLong()
    val gameInfo = GameInfo(testGame, testGame, testGame)
    msgServer.msgService.doCreateGame(gameInfo)
}