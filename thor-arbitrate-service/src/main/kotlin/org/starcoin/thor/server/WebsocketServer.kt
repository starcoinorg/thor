package org.starcoin.thor.server

import com.google.common.base.Preconditions
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
import org.starcoin.lightning.client.HashUtils
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import org.starcoin.lightning.client.core.Invoice
import org.starcoin.thor.core.*
import org.starcoin.thor.utils.decodeBase58
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
                    val currentUser = User(this, randomString())
                    msgService.doConnection(currentUser)
                    try {
                        //TODO
                        this.send(Frame.Text(WsMsg(MsgType.CONN, SessionId(currentUser.sessionId).data2Str()).msg2Str()))
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val msg = frame.readText()
                                println(msg)

                                launch {
                                    try {
                                        val wsMsg = WsMsg.str2WsMsg(msg)
                                        receivedMessage(wsMsg, currentUser)
                                    } catch (e: Exception) {
                                        sendError(currentUser.session, e)
                                    }
                                }
                            }
                        }
                    } finally {
                        LOG.info("${currentUser.sessionId} finish,clear room")
                        currentUser.currentRoom?.let { msgService.getRoomOrNull(it) }?.players?.remove(currentUser.sessionId)
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

    private fun receivedMessage(msg: WsMsg, currentUser: User) {
        when (msg.type) {
            MsgType.CONFIRM_REQ -> {
                val ci = msgService.queryConfirmInfo(currentUser.sessionId)
                val resp = when (ci) {
                    null -> {
                        val sc = msgService.doConfirm(currentUser.sessionId)

                        val invoice = Invoice(HashUtils.hash160(sc.rhash.decodeBase58()), 1)
                        val inviteResp = syncClient.addInvoice(invoice)
                        msgService.doConfirmInfo(currentUser.sessionId, inviteResp.paymentRequest)
                        ConfirmResp(inviteResp.paymentRequest)
                    }
                    else ->
                        ConfirmResp(ci.paymentStr)
                }
                GlobalScope.launch {
                    currentUser.session.send(Frame.Text(WsMsg(MsgType.CONFIRM_RESP, resp.data2Str()).msg2Str()))
                }
            }
            MsgType.CONFIRM_PAYMENT_REQ -> {
                val resp = msg.str2Data(ConfirmPaymentReq::class)
                var invoice: Invoice
                do {
                    invoice = syncClient.lookupInvoice(resp.paymentHash)
                } while (!invoice.invoiceDone())

                if (invoice.state == Invoice.InvoiceState.SETTLED) {
                    msgService.doPaymentSettled(currentUser.sessionId)
                }
            }
            MsgType.CREATE_ROOM_REQ -> {
                val req = msg.str2Data(CreateRoomReq::class)
                val data = msgService.doCreateRoom(req.gameHash, req.deposit)
                GlobalScope.launch {
                    currentUser.session.send(Frame.Text(WsMsg(MsgType.CREATE_ROOM_RESP, CreateRoomResp(data.id).data2Str()).msg2Str()))
                }
            }
            MsgType.JOIN_ROOM_FREE -> {
                if (currentUser.currentRoom != null) {
                    throw RuntimeException("${currentUser.sessionId} has in room ${currentUser.currentRoom}")
                }
                val req = msg.str2Data(JoinRoomReq::class)
                var room = msgService.getRoom(req.roomId)
                assert(!room.payment)
                room = msgService.doJoinRoom(currentUser.sessionId, req.roomId)
                if (room.isFull) {
                    msgService.doGameBegin(Pair(room.players[0], room.players[1]), req.roomId)
                }
                currentUser.currentRoom = room.id
            }
            MsgType.JOIN_ROOM_PAY -> {
                if (currentUser.currentRoom != null) {
                    throw RuntimeException("${currentUser.sessionId} has in room ${currentUser.currentRoom}")
                }
                val req = msg.str2Data(JoinRoomReq::class)
                var room = msgService.getRoom(req.roomId)
                assert(room.payment)
                assert(room.cost > 0)
                val ci = msgService.queryConfirmInfo(currentUser.sessionId)
                assert(ci!!.confirmed)
                room = msgService.doJoinRoom(currentUser.sessionId, req.roomId)
                if (room.isFull) {
                    msgService.doPayments(Pair(room.players[0], room.players[1]), req.roomId, room.cost)
                }
                currentUser.currentRoom = room.id
            }
            MsgType.PAYMENT_RESP -> {
                val room = msgService.getRoom(msg.to!!)
                room.players.filter { it != currentUser.sessionId }.apply {
                    msgService.doRoomPaymentMsg(this, msg)
                }
            }
            MsgType.PAYMENT_START_REQ -> {
                val room = msgService.getRoom(msg.to!!)
                room.players.filter { it != currentUser.sessionId }.apply {
                    msgService.doRoomPaymentMsg(this, msg)
                }
            }
            MsgType.PAYMENT_START_RESP -> {
                val req = msg.str2Data(PaymentAndStartResp::class)
                var room = msgService.doPayment(currentUser.sessionId, req.roomId)
                if (room.isFullPayment) {
                    msgService.doGameBegin(Pair(room.players[0], room.players[1]), req.roomId)
                }
            }
            MsgType.SURRENDER_REQ -> {
                val req = msg.str2Data(SurrenderReq::class)
                msgService.doSurrender(currentUser.sessionId, req.roomId)
            }
            MsgType.CHALLENGE_REQ -> {
                val req = msg.str2Data(ChallengeReq::class)
                msgService.doChallenge(currentUser.sessionId, req.instanceId)
            }
            MsgType.ROOM_DATA_MSG -> {
                val room = msgService.getRoom(msg.to!!)
                room.players.filter { it != currentUser.sessionId }.apply {
                    msgService.doBroadcastRoomMsg(currentUser.sessionId, this, msg.data)
                }
            }
            else -> {
                msgService.doOther(msg)
            }
        }
    }
}