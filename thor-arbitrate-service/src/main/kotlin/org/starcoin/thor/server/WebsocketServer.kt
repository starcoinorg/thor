package org.starcoin.thor.server

import io.grpc.BindableService
import io.grpc.Channel
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.Sessions
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import org.starcoin.thor.core.*
import org.starcoin.thor.utils.randomString

class WebsocketServer(private val lnConfig: LnConfig) : RpcServer<BindableService> {

    companion object {
        val LOG = LoggerFactory.getLogger(WebsocketServer::class.java)
    }

    lateinit var engine: ApplicationEngine
    var msgService = MsgServiceImpl()

    private lateinit var chan: Channel
    lateinit var syncClient: SyncClient

    override fun start() {
        // lightning network channel
        chan = Utils.buildChannel(lnConfig.cert, lnConfig.host, lnConfig.port)
        syncClient = SyncClient(chan)

        engine = embeddedServer(Netty, 8082) {
            install(DefaultHeaders) {
                header(HttpHeaders.Server, "Thor")
            }
            install(CallLogging) {
                level = Level.DEBUG
            }
            install(WebSockets)
            install(CORS) {
                anyHost()
                allowCredentials = true
            }
            install(ContentNegotiation) {
                jackson {
                    // TODO("config jackson")
                }
            }
            install(Sessions) {
                //TODO("add cookie")
            }
            intercept(ApplicationCallPipeline.Features) {
            }
            routing {
                get("/h") {
                    call.respond(mapOf("hello world" to true))
                }
                post("/p") {
                    val post = call.receive<HttpMsg>()
                    when (post.type) {
                        HttpType.CREATE_GAME -> {
                            val msg = post.str2Data(CreateGameReq::class)
                            val gameInfo = GameInfo(msg.gameHash, msg.gameHash, msg.gameHash)
                            msgService.doCreateGame(gameInfo)
                            call.respond(gameInfo)
                        }
                        HttpType.GAME_LIST -> {
                            val msg = post.str2Data(GameListReq::class)
                            val data = msgService.doGameList(msg.page)

                            call.respond(data)
                        }
                        HttpType.CREATE_ROOM -> {
                            val msg = post.str2Data(CreateRoomReq::class)
                            val data = msgService.doCreateRoom(msg.gameHash, msg.deposit)
                            call.respond(CreateRoomResp(data.id))
                        }
                        HttpType.ROOM_LIST -> {
                            //val msg = post.str2Data(RoomListReq::class)
                            val data = msgService.doRoomList()
                            call.respond(RoomListResp(data))
                        }
                        HttpType.ROOM -> {
                            val msg = post.str2Data(GetRoomReq::class)
                            val room = msgService.getRoom(msg.roomId)
                            call.respond(room)
                        }
                    }
                }
                webSocket("/ws") {
                    val newUser = User(this, randomString())
                    msgService.doConnection(newUser)
                    try {
                        //TODO
                        this.send(Frame.Text(WsMsg("admin", newUser.sessionId, MsgType.CONN, "{\"id\":\"${newUser.sessionId}\"}").msg2Str()))
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val msg = frame.readText()
                                println(msg)

                                launch {
                                    try {
                                        val wsMsg = WsMsg.str2WsMsg(msg)
                                        receivedMessage(wsMsg, newUser)
                                    } catch (e: Exception) {
                                        sendError(newUser.session, e)
                                    }
                                }
                            }
                        }
                    } finally {
                        LOG.info("${newUser.sessionId} finish,clear room")
                        newUser.currentRoom?.let { msgService.getRoomOrNull(it) }?.players?.remove(newUser.sessionId)
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

    private fun sendError(session: DefaultWebSocketSession, exception: Exception) {
        //TODO
    }

    private fun sendError(user: User, msg: String) {
        LOG.error("${user.sessionId} $msg")
    }

    private fun receivedMessage(msg: WsMsg, tmpUser: User) {
        when (msg.type) {
            MsgType.CONN -> {

            }
//            MsgType.START_INVITE_REQ -> {
//                val req = msg.str2Data(StartAndInviteReq::class)
//                val resp = msgService.doStartAndInvite(req.gameHash, tmpUser.sessionId, msg.to)
//                GlobalScope.launch {
//                    tmpUser.session.send(Frame.Text(WsMsg(msg.to, tmpUser.sessionId, MsgType.START_INVITE_RESP, resp.data2Str()).msg2Str()))
//                }
//            }
            MsgType.PAYMENT_START_RESP -> {
                val req = msg.str2Data(PaymentAndStartResp::class)
                msgService.doGameBegin(msg.from, msg.to, req)
            }
            MsgType.SURRENDER_REQ -> {
                val rep = msg.str2Data(SurrenderReq::class)
                msgService.doSurrender(tmpUser.sessionId, rep.instanceId)
            }
            MsgType.CHALLENGE_REQ -> {
                val rep = msg.str2Data(ChallengeReq::class)
                msgService.doChallenge(tmpUser.sessionId, rep.instanceId)
            }
            MsgType.JOIN_ROOM -> {
                if (tmpUser.currentRoom != null) {
                    throw RuntimeException("${tmpUser.sessionId} has in room ${tmpUser.currentRoom}")
                }
                val rep = msg.str2Data(JoinRoomReq::class)
                val room = msgService.doJoinRoom(tmpUser.sessionId, rep.roomId)
                if (room.isFull) {
                    msgService.doGameBegin2(Pair(room.players[0], room.players[1]), rep.roomId)
                }
                tmpUser.currentRoom = room.id
            }
            MsgType.ROOM_DATA_MSG -> {
                val room = msgService.getRoom(msg.to)
                room.players.filter { it != tmpUser.sessionId }.apply {
                    msgService.doBroadcastRoomMsg(tmpUser.sessionId, this, msg.data)
                }
            }
            MsgType.CREATE_ROOM_REQ -> {
                val req = msg.str2Data(CreateRoomReq::class)
                val data = msgService.doCreateRoom(req.gameHash, req.deposit)
            }
            else -> {
                msgService.doOther(msg)
            }
        }
    }
}