package org.starcoin.thor.server

import org.starcoin.thor.core.LnClient
import org.starcoin.thor.core.LnConfig

fun main(args: Array<String>) {
    val adminCert = WebsocketServer::class.java.classLoader.getResourceAsStream("arb.cert")
    val adminConfig = LnConfig("3333", adminCert, "starcoin-firstbox", 20009)
    val adminClient = LnClient(adminConfig)

    val gameManager = GameManager(adminConfig.addr)
    val server = GrpcServer(gameManager)
    server.start()

    val msgServer = WebsocketServer(gameManager, adminClient)
    msgServer.start()

    val runtime = Runtime.getRuntime()
    runtime.addShutdownHook(Thread(server::stop))
    runtime.addShutdownHook(Thread(msgServer::stop))
}