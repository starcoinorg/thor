package org.starcoin.thor.server

import io.grpc.BindableService
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.request.receive
import io.ktor.response.respond
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
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.*
import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.doVerify
import org.starcoin.thor.utils.randomString
import java.security.KeyPair

data class CurrentSession(val sessionId: String, val socket: DefaultWebSocketSession)

class WebsocketServer(private val self: UserSelf, private val gameManager: GameManager, private val roomManager: RoomManager) : RpcServer<BindableService> {
    constructor(path: String, gameManager: GameManager, roomManager: RoomManager) : this(UserSelf.paseFromKeyPair(SignService.generateKeyPair()), gameManager, roomManager)//TODO

    constructor(keyPair: KeyPair, gameManager: GameManager, roomManager: RoomManager) : this(UserSelf.paseFromKeyPair(keyPair), gameManager, roomManager)

    constructor(gameManager: GameManager, roomManager: RoomManager) : this(UserSelf.paseFromKeyPair(SignService.generateKeyPair()), gameManager, roomManager)

    companion object {
        val LOG = LoggerFactory.getLogger(WebsocketServer::class.java)
    }

    lateinit var engine: ApplicationEngine
    private var gameService = GameServiceImpl(gameManager, roomManager)
    private val playService = PlayServiceImpl(gameManager, roomManager)

    override fun start() {
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
                register(ContentType.Application.Json, JsonSerializableConverter())
            }
            install(Sessions) {

            }
            intercept(ApplicationCallPipeline.Features) {
            }
            routing {
                post("/p") {
                    val post = call.receive<HttpMsg>()
                    when (post.type) {
                        HttpType.PUB_KEY -> {
                            call.respond(PubKeyResp(ByteArrayWrapper(self.userInfo.publicKey.encoded)))
                        }
                        HttpType.CREATE_GAME -> {
                            val gameInfo = post.data as CreateGameReq
                            call.respond(gameService.createGame(gameInfo))
                        }
                        HttpType.GAME_LIST -> {
                            val msg = post.data as GameListReq
                            val data = gameService.gameList(msg.page)
                            call.respond(data)
                        }
                        HttpType.CREATE_ROOM -> {
                            val msg = post.data as CreateRoomReq
                            val data = gameService.doCreateRoom(msg.gameHash, msg.deposit)
                            call.respond(CreateRoomResp(data.roomId))
                        }
                        HttpType.ROOM_LIST -> {
                            val msg = post.data as RoomListByGameReq
                            val data = gameService.doRoomList(msg.gameId)
                            call.respond(RoomListByGameResp(data))
                        }
                        HttpType.ALL_ROOM_LIST -> {
                            val msg = post.data as RoomListReq
                            val data = gameService.doRoomList(msg.page)
                            call.respond(RoomListResp(data))
                        }
                        HttpType.ROOM -> {
                            val msg = post.data as GetRoomReq
                            val room = gameService.getRoom(msg.roomId)
                            call.respond(room)
                        }
                    }
                }
                webSocket("/ws") {
                    val current = CurrentSession(randomString(), this)

                    val nonce = playService.sendNonce(current.sessionId, current.socket)
                    current.socket.send(doSign(MsgType.NONCE, Nonce(nonce, ByteArrayWrapper(self.userInfo.publicKey.encoded))))

                    try {
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val msg = frame.readText()
                                println("===========")
                                println(msg)
                                val signMsg = MsgObject.fromJson(msg, SignMsg::class)

                                if (!doVerify(current.sessionId, signMsg)) {
                                    playService.clearSession(current.sessionId)
                                    current.socket.close()
                                } else {
                                    launch {
                                        try {
                                            receivedMessage(signMsg.msg, current)
                                        } catch (e: Exception) {
                                            //sendError(currentUser.session, e)
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
//                        LOG.info("${currentSessionId} finish,clear room")
//                        currentSessionId?.let { msgService.getRoomOrNull(it) }?.players?.remove(currentUser.sessionId)
                        //TODO("remove session")
                    }
                }
            }
        }.start(true)
    }

    private fun doSign(type: MsgType, data: Data): Frame.Text {
        val msg = WsMsg(type, self.userInfo.id, data)
        return playService.doSign(msg, self.privateKey)
    }

    private fun doVerify(sessionId: String, signMsg: SignMsg): Boolean {
        when (signMsg.msg.type) {
            MsgType.NONCE -> {
                val resp = signMsg.msg.data as Nonce
                var flag = playService.compareNoce(sessionId, resp.nonce)
                if (flag) {
                    val pk = SignService.toPubKey(resp.pubKey.bytes)
                    flag = SignService.doVerify(signMsg, pk)
                    if (flag) {
                        playService.storePubKey(sessionId, UserInfo(pk))
                    }
                }
                return flag
            }
            else -> {
                //get pubkey from SessionId
                val pk = playService.queryPubKey(sessionId)
                pk?.let {
                    return SignService.doVerify(signMsg, pk)
                }
                return false
            }
        }
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

    //    private fun sendError(user: User, msg: String) {
//        LOG.error("${user.sessionId} $msg")
//    }
//
    private fun receivedMessage(msg: WsMsg, current: CurrentSession) {
        when (msg.type) {
            MsgType.CREATE_ROOM_REQ -> {
                val req = msg.data as CreateRoomReq
                val data = playService.doCreateRoom(req.gameHash, req.deposit, req.time, current.sessionId)
                GlobalScope.launch {
                    data?.let { current.socket.send(doSign(MsgType.CREATE_ROOM_RESP, CreateRoomResp(data.roomId))) }
                }
            }
            MsgType.JOIN_ROOM_REQ -> {
                val req = msg.data as JoinRoomReq
                playService.doJoinRoom(current.sessionId, req.roomId, self)
            }
            MsgType.HASH_RESP -> {
//                val req = msg.data as HashResp
//                val room = msgService.getRoom(msg.userId)
//                room.players.filter { it != currentUser.sessionId }.apply {
//                    msgService.doRoomPaymentMsg(this, msg)
//                }
            }
//            MsgType.PAYMENT_START_REQ -> {
//                val room = msgService.getRoom(msg.userId)
//                room.players.filter { it != currentUser.sessionId }.apply {
//                    msgService.doRoomPaymentMsg(this, msg)
//                }
//            }
//            MsgType.PAYMENT_START_RESP -> {
//                val req = msg.data as PaymentAndStartResp
//                var room = msgService.doPayment(currentUser.sessionId, req.roomId)
//                if (room.isFullPayment) {
//                    msgService.doGameBegin(Pair(room.players[0], room.players[1]), req.roomId)
//                }
//            }
//            MsgType.SURRENDER_REQ -> {
//                val req = msg.data as SurrenderReq
//                msgService.doSurrender(currentUser.sessionId, req.roomId)
//            }
//            MsgType.CHALLENGE_REQ -> {
//                val req = msg.data as ChallengeReq
//                msgService.doChallenge(currentUser.sessionId, req.instanceId)
//            }
//            MsgType.ROOM_DATA_MSG -> {
//                val room = msgService.getRoom(msg.userId)
//                room.players.filter { it != currentUser.sessionId }.apply {
//                    msgService.doBroadcastRoomMsg(currentUser.sessionId, this, msg.data)
//                }
//            }
//            else -> {
//                msgService.doOther(msg)
//            }
        }
    }
}