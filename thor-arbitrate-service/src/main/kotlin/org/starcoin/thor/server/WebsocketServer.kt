package org.starcoin.thor.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.grpc.BindableService
import io.ktor.application.ApplicationCallPipeline
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import io.ktor.routing.routing
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.engine.ApplicationEngine

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.starcoin.thor.core.ConnData
import org.starcoin.thor.core.MsgType
import org.starcoin.thor.core.WsMsg

class WebsocketServer : RpcServer<BindableService> {
    lateinit var engine: ApplicationEngine
    val msgService = MsgServiceImpl()
    val om = ObjectMapper().registerModule(KotlinModule())


    override fun start() {
        engine = embeddedServer(Netty, 8082) {
            install(DefaultHeaders)
            install(CallLogging)
            install(WebSockets)
            install(ContentNegotiation) {
//                jackson {
//                    // TODO("config jackson")
//                }
//                register(ContentType.Application.Json, JacksonConverter())
            }
            install(Sessions) {
                //TODO("add cookie")
            }
            intercept(ApplicationCallPipeline.Features) {
                //TODO("verify address，add session")
            }
            routing {
                webSocket("/ws") {
                    try {
                        incoming.consumeEach {
                            frame ->
                            if (frame is Frame.Text) {
                                val msg = frame.readText()
                                println("$msg")

                                launch {
                                    receivedMessage(om.readValue(msg, WsMsg::class.java))
                                }
                            }
                        }
                    } finally {
                        //TODO("remove session")
                    }
                }
            }
        }.start(true)
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

    private fun receivedMessage(msg:WsMsg) {
        //TODO("link to business service")

        when {
            msg.type == MsgType.CONN -> {
                om.readValue(msg.data, ConnData::class.java)
                msgService.test()
            } else -> msgService.test2()
        }
    }
}