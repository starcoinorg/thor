package org.starcoin.thor.server

import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager

fun main(args: Array<String>) {
    val gameManager = GameManager()
    val roomManager = RoomManager()
    val websocketServer = WebsocketServer(gameManager)
    websocketServer.start()
}