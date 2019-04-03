package org.starcoin.thor.server

fun main(args: Array<String>) {
    var server = GrpcServer()
    server.start()

    var msgServer = WebsocketServer()
    msgServer.start()

    val runtime = Runtime.getRuntime()
    runtime.addShutdownHook(Thread(server::stop))
    runtime.addShutdownHook(Thread(msgServer::stop))
}