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
import java.util.*

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
                    val newUser = User(this, randomString())
                    msgService.doConnection(newUser)
                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val msg = frame.readText()
                                println(msg)

                                launch {
                                    val wsMsg = WsMsg.str2WsMsg(msg)

                                    receivedMessage(wsMsg, newUser)
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

    private fun receivedMessage(msg: WsMsg, tmpUser: User) {
        when {
            msg.type == MsgType.CONN -> {

            }
            msg.type == MsgType.START_INVITE_REQ -> {
                val req = msg.str2Data(StartAndInviteReq::class)
                val resp = msgService.doStartAndInvite(req.gameHash, tmpUser.sessionId!!, msg.to)
                GlobalScope.launch {
                    tmpUser.session.send(Frame.Text(WsMsg(msg.to, tmpUser.sessionId!!, MsgType.START_INVITE_RESP, resp.data2Str()).msg2Str()))
                }
            }
            msg.type == MsgType.PAYMENT_START_RESP -> {
                val req = msg.str2Data(PaymentAndStartResp::class)
                msgService.doGameBegin(msg.from, msg.to, req)
            }
            msg.type == MsgType.SURRENDER_REQ -> {
                val rep = msg.str2Data(SurrenderReq::class)
                msgService.doSurrender(tmpUser.sessionId!!, rep.instanceId)
            }
            msg.type == MsgType.CHALLENGE_REQ -> {
                val rep = msg.str2Data(ChallengeReq::class)
                msgService.doChallenge(tmpUser.sessionId!!, rep.instanceId)
            }
            else -> {
                msgService.doOther(msg)
            }
        }
    }
}