package org.starcoin.thor.server

import org.starcoin.thor.core.LnConfig

fun main(args: Array<String>) {
    val arbiterCert = WebsocketServer::class.java.classLoader.getResourceAsStream("arb.cert")
    val arbiterConfig = LnConfig(arbiterCert, "starcoin-firstbox", 20009)

    val msgServer = WebsocketServer(arbiterConfig)
    msgServer.start()
}