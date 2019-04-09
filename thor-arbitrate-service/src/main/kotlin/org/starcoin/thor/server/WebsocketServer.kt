package org.starcoin.thor.server

import io.grpc.BindableService
import io.ktor.application.ApplicationCallPipeline
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import io.ktor.routing.routing
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.server.engine.ApplicationEngine

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.starcoin.thor.core.*

class WebsocketServer(private val gameManager: GameManager, private val lnClient: LnClient) : RpcServer<BindableService> {

    lateinit var engine: ApplicationEngine
    lateinit var msgService: MsgServiceImpl
    private val userManager = UserManager()

    override fun start() {
        msgService = MsgServiceImpl(userManager, gameManager)

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

            }
            routing {
                webSocket("/ws") {
                    val tmpUser = TmpUser(this)
                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val msg = frame.readText()

                                launch {
                                    val wsMsg = WsMsg.str2WsMsg(msg)
                                    var flag = false
                                    when (wsMsg.type == MsgType.CONN) {
                                        true -> {
                                            flag = true
                                        }
                                        false -> {
                                            //TODO("verify user info")
                                            tmpUser.addr?.let { flag = true }
                                        }
                                    }

                                    if (flag) {
                                        receivedMessage(wsMsg, tmpUser)
                                    }
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

    private fun receivedMessage(msg: WsMsg, tmpUser: TmpUser) {
        //TODO("link to business service")
        when {
            msg.type == MsgType.CONN -> {
                val connData = msg.str2Data(ConnData::class)

                val createFlag = msgService.doConnection(msg.from, connData, tmpUser.session)
                if (createFlag) {
                    tmpUser.addr = msg.from
                }
            }
            msg.type == MsgType.START_INVITE_REQ -> {
                val req = msg.str2Data(StartAndInviteReq::class)
                val resp = msgService.doStartAndInvite(req.gameHash, tmpUser.addr!!, msg.to)
                GlobalScope.launch {
                    tmpUser.session.send(Frame.Text(WsMsg(msg.to, tmpUser.addr!!, MsgType.START_INVITE_RESP, resp.data2Str()).msg2Str()))
                }
            }
            msg.type == MsgType.SURRENDER_REQ -> {
                val rep = msg.str2Data(SurrenderReq::class)
                msgService.doSurrender(tmpUser.addr!!, rep.instanceId)
            }
            msg.type == MsgType.CHALLENGE_REQ -> {
                val rep = msg.str2Data(ChallengeReq::class)
                msgService.doChallenge(tmpUser.addr!!, rep.instanceId)
            }
            else -> {
                msgService.doOther(msg)
            }
        }
    }
}

data class TmpUser(val session: DefaultWebSocketSession, var addr: String? = null)