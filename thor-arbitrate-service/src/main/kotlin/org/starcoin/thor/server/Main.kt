package org.starcoin.thor.server

import org.starcoin.thor.manager.GameManager

fun main(args: Array<String>) {
    val gameManager = GameManager()
    val websocketServer = WebsocketServer(gameManager)
    websocketServer.start()
}