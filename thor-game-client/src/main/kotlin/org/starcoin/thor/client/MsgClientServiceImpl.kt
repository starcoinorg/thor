package org.starcoin.thor.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.base.Preconditions
import io.grpc.Channel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.websocket.ClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import org.starcoin.lightning.client.core.Invoice
import org.starcoin.lightning.client.core.Payment
import org.starcoin.thor.core.*
import java.util.*

class MsgClientServiceImpl(private val lnConfig: LnConfig) {

    private lateinit var session: ClientWebSocketSession
    private lateinit var gameInstanceId: String
    private val rSet = Collections.synchronizedSet(mutableSetOf<String>())

    private lateinit var chan: Channel
    private lateinit var syncClient: SyncClient

    private lateinit var client: HttpClient

    private val msgChannel = kotlinx.coroutines.channels.Channel<String>(10)

    private val json = jacksonObjectMapper()
    fun start() {
        // lightning network channel
        chan = Utils.buildChannel(lnConfig.cert, lnConfig.host, lnConfig.port)
        syncClient = SyncClient(chan)

        client = HttpClient(CIO).config {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
            install(WebSockets)
        }
        GlobalScope.launch {
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8082, path = "/ws") {
                session = this
                doConnection()

                for (message in incoming.map { it as? Frame.Text }.filterNotNull()) {
                    val msg = message.readText()
                    val resp = WsMsg.str2WsMsg(msg)
                    doMsg(resp)
                }
            }
        }
    }

    private fun doMsg(msg: WsMsg) {
        when (msg.type) {
            MsgType.INVITE_PAYMENT_REQ -> {
                val ipr = msg.str2Data(InvitedAndPaymentReq::class)
                doInvitedAndPayment(msg.from!!, ipr)
            }
            MsgType.CREATE_ROOM_RESP -> {
                val crr = msg.str2Data(CreateRoomResp::class)
                GlobalScope.launch {
                    msgChannel.send(crr.roomId!!)
                }
            }
            MsgType.INVITE_PAYMENT_RESP -> {
                val ipr = msg.str2Data(InvitedAndPaymentResp::class)
                doSendPayment(msg.from!!, ipr)
            }
            MsgType.PAYMENT_START_REQ -> {
                val psr = msg.str2Data(PaymentAndStartReq::class)
                doPaymentStart(psr)
            }
            MsgType.GAME_BEGIN -> {
                println("todo : game begin")
                //TODO("game begin")
            }
            MsgType.SURRENDER_RESP -> {
                val sr = msg.str2Data(SurrenderResp::class)
                println("i win the game !!!")
                rSet.add(sr.r)
            }
            MsgType.ROOM_DATA_MSG -> {
                println("i get the ${msg.from} msg: ${msg.data}")
            }
        }
    }

    fun joinRoom(roomId: String) {
        val msg = JoinRoomReq(roomId).data2Str()
        doSend(WsMsg(MsgType.JOIN_ROOM, msg).msg2Str())
    }

    fun createGame(): String {
        val defaultGame = "test-" + Random().nextLong()
        println(defaultGame)
        createGame(defaultGame)
        return defaultGame
    }

    fun createGame(gameName: String) {
        runBlocking {
            client.post<String>(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(json.writeValueAsString(HttpMsg(HttpType.CREATE_GAME, CreateGameReq(gameName).data2Str())), contentType = ContentType.Application.Json))
        }
    }

    fun queryGameList(): GameListResp? {
        return queryGameList(1)
    }

    fun roomMsg(roomId: String, msg: String) {
        doSend(WsMsg(MsgType.ROOM_DATA_MSG, msg, from = null, to = roomId).msg2Str())
    }

    fun queryGameList(page: Int): GameListResp? {
        var games = GameListResp(0, null)
        runBlocking {
            games = client.post(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(json.writeValueAsString(HttpMsg(HttpType.GAME_LIST, GameListReq(1).data2Str())), contentType = ContentType.Application.Json))
        }

        return games
    }

    fun createRoom(gameName: String, deposit: Long = 0): CreateRoomResp {
        Preconditions.checkArgument(deposit >= 0)
        var resp = CreateRoomResp(null)
        runBlocking {
            resp = client.post(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(json.writeValueAsString(HttpMsg(HttpType.CREATE_ROOM, CreateRoomReq(gameName, deposit).data2Str())), contentType = ContentType.Application.Json))
        }

        return resp
    }

    fun queryRoomList(gameName: String): RoomListResp? {
        var resp = RoomListResp(null)
        runBlocking {
            resp = client.post(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(json.writeValueAsString(HttpMsg(HttpType.ROOM_LIST, RoomListReq(gameName).data2Str())), contentType = ContentType.Application.Json))
        }

        return resp
    }

    private fun doConnection() {
        val data = ConnData().data2Str()
//        val conn = WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.CONN, data).msg2Str()
//        doSend(conn)
    }

    fun doStartAndInviteReq(gameHash: String, toAddr: String) {
        val sai = StartAndInviteReq(gameHash).data2Str()
//        val msg = WsMsg(lnClient.conf.addr!!, toAddr, MsgType.START_INVITE_REQ, sai).msg2Str()
//        doSend(msg)
    }

    fun doCreateRoom(gameName: String, deposit: Long = 0) {
        Preconditions.checkArgument(deposit >= 0)
        doSend(WsMsg(MsgType.CREATE_ROOM_REQ, CreateRoomReq(gameName, deposit).data2Str()).msg2Str())
    }

    fun doSurrenderReq() {
        val rep = SurrenderReq(gameInstanceId).data2Str()
//        val msg = WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.SURRENDER_REQ, rep).msg2Str()
//        doSend(msg)
    }

    fun doChallenge() {
        val rep = ChallengeReq(gameInstanceId).data2Str()
//        val msg = WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.CHALLENGE_REQ, rep).msg2Str()
//        doSend(msg)
    }

    private fun doInvitedAndPayment(fromAddr: String, ipr: InvitedAndPaymentReq) {
//        val gameInfo = gameClient.queryGame(ipr.gameHash!!)
//        gameInfo?.let {
//            val invoice = Invoice(HashUtils.hash160(ipr.rhash.decodeBase58()), gameInfo.cost)
//            val inviteResp = lnClient.syncClient.addInvoice(invoice)
//            gameInstanceId = ipr.instanceId!!
//            doSend(WsMsg(lnClient.conf.addr!!, fromAddr, MsgType.INVITE_PAYMENT_RESP, InvitedAndPaymentResp(gameInstanceId, inviteResp.paymentRequest).data2Str()).msg2Str())
//        }
    }

    private fun doStartAndInviteResp(fromAddr: String, sir: StartAndInviteResp) {
        if (sir.succ) {
            doInvitedAndPayment(fromAddr, sir.iap!!)
        }
    }

    private fun doSendPayment(addr: String, ipr: InvitedAndPaymentResp) {
        val payment = Payment(ipr.paymentRequest)
//        val resp = lnClient.syncClient.sendPayment(payment)
//        when (resp.paymentError.isEmpty()) {
//            true -> {
//                val psr = PaymentAndStartReq(ipr.instanceId, resp.paymentHash).data2Str()
//                doSend(WsMsg(lnClient.conf.addr!!, addr, MsgType.PAYMENT_START_REQ, psr).msg2Str())
//            }
//            else -> {
//                println(resp.paymentError)
//            }
//        }
    }

    private fun doPaymentStart(psr: PaymentAndStartReq) {
        var invoice: Invoice
//        GlobalScope.launch {
//            do {
//                invoice = lnClient.syncClient.lookupInvoice(psr.paymentHash)
//            } while (!invoice.invoiceDone())
//
//            if (invoice.state == Invoice.InvoiceState.SETTLED) {
//                val psr = PaymentAndStartResp(psr.instanceId).data2Str()
//                doSend(WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.PAYMENT_START_RESP, psr).msg2Str())
//            }
//        }
    }

    private fun doSend(msg: String) {
        GlobalScope.launch { session.send(Frame.Text(msg)) }
    }

    fun channelMsg(): String {
        val msg = runBlocking {
            msgChannel.receive()
        }

        return msg
    }

}