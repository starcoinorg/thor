package org.starcoin.thor.server

import io.grpc.BindableService
import io.ktor.application.ApplicationCallPipeline
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import io.ktor.routing.routing
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.engine.ApplicationEngine

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class WebsocketServer : RpcServer<BindableService> {
    lateinit var engine: ApplicationEngine
    val msgService = MsgServiceImpl()

    override fun start() {
        engine = embeddedServer(Netty, 8082) {
            install(DefaultHeaders)
            install(CallLogging)
            install(WebSockets)
            install(Sessions) {
                //TODO("add cookie")
            }
            intercept(ApplicationCallPipeline.Features) {
                //TODO("verify address，add session")
            }
            routing {
                webSocket("/ws") {
                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                launch {
                                    val msg = frame.readText()
                                    println("$msg")
                                    receivedMessage(msg)
                                }
                            }
                        }
                    } finally {
                        //TODO("remove session")
                    }
                }
            }
        }.start()
    }

    override fun stop() {
        TODO("not implemented")
    }

    override fun registerService(service: BindableService) {
        TODO("not implemented")
    }

    override fun awaitTermination() {
        TODO("not implemented")
    }

    private fun receivedMessage(command: String) {
        //TODO("link to business service")

        when {
            command.startsWith("/test1") -> msgService.test()
            else -> msgService.test2()
        }
    }
}