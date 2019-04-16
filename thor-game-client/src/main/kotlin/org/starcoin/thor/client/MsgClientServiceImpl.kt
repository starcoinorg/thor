package org.starcoin.thor.client

import com.google.common.base.Preconditions
import io.grpc.Channel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.ClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.starcoin.lightning.client.HashUtils
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import org.starcoin.lightning.client.core.Invoice
import org.starcoin.lightning.client.core.Payment
import org.starcoin.thor.core.*
import org.starcoin.thor.utils.decodeBase58
import java.util.*

class MsgClientServiceImpl(private val lnConfig: LnConfig) {

    private lateinit var session: ClientWebSocketSession
    private lateinit var roomId: String
    private val rSet = Collections.synchronizedSet(mutableSetOf<String>())

    private lateinit var chan: Channel
    private lateinit var syncClient: SyncClient

    private lateinit var client: HttpClient

    private val msgChannel = kotlinx.coroutines.channels.Channel<String>(10)
    private lateinit var sessionId: String

    fun start() {
        // lightning network channel
        chan = Utils.buildChannel(lnConfig.cert, lnConfig.host, lnConfig.port)
        syncClient = SyncClient(chan)

        client = HttpClient(CIO).config {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(WebSockets)
        }
        GlobalScope.launch {
            client.ws(method = HttpMethod.Get, host = "127.0.0.1", port = 8082, path = "/ws") {
                session = this

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
            MsgType.CONN -> {
                val si = msg.str2Data(SessionId::class)
                sessionId = si.id
            }
            MsgType.CONFIRM_RESP -> {
                val cr = msg.str2Data(ConfirmResp::class)
                doConfirmPaymentReq(cr.paymentRequest)
            }
            MsgType.CREATE_ROOM_RESP -> {
                val crr = msg.str2Data(CreateRoomResp::class)
                GlobalScope.launch {
                    msgChannel.send(crr.roomId!!)
                }
            }
            MsgType.PAYMENT_REQ -> {
                val pr = msg.str2Data(PaymentReq::class)
                doPayment(pr)
            }
            MsgType.PAYMENT_RESP -> {
                val pr = msg.str2Data(PaymentResp::class)
                doSendPayment(pr)
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

    fun joinFreeRoom(roomId: String) {
        val msg = JoinRoomReq(roomId).data2Str()
        doSend(WsMsg(MsgType.JOIN_ROOM_FREE, msg).msg2Str())
    }

    fun joinPayRoom(roomId: String) {
        val msg = JoinRoomReq(roomId).data2Str()
        doSend(WsMsg(MsgType.JOIN_ROOM_PAY, msg).msg2Str())
    }

    fun createGame(): String {
        val defaultGame = "test-" + Random().nextLong()
        println(defaultGame)
        createGame(defaultGame)
        return defaultGame
    }

    fun createGame(gameName: String) {
        runBlocking {
            client.post<String>(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(HttpMsg(HttpType.CREATE_GAME, CreateGameReq(gameName).data2Str()).toJson(), contentType = ContentType.Application.Json))
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
            games = client.post(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(HttpMsg(HttpType.GAME_LIST, GameListReq(page).data2Str()).toJson(), contentType = ContentType.Application.Json))
        }

        return games
    }

    fun createRoom(gameName: String, deposit: Long = 0): CreateRoomResp {
        Preconditions.checkArgument(deposit >= 0)
        var resp = CreateRoomResp(null)
        runBlocking {
            resp = client.post(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(HttpMsg(HttpType.CREATE_ROOM, CreateRoomReq(gameName, deposit).data2Str()).toJson(), contentType = ContentType.Application.Json))
        }

        return resp
    }

    fun queryRoomList(gameName: String): RoomListResp? {
        var resp = RoomListResp(null)
        runBlocking {
            resp = client.post(host = "127.0.0.1", port = 8082, path = "/p", body = TextContent(HttpMsg(HttpType.ROOM_LIST, RoomListReq(gameName).data2Str()).toJson(), contentType = ContentType.Application.Json))
        }

        return resp
    }

    fun doConfirmReq() {
        doSend(WsMsg(MsgType.CONFIRM_REQ, ConfirmReq().data2Str()).msg2Str())
    }

    fun doConfirmPaymentReq(paymentRequest: String) {
        val payment = Payment(paymentRequest)
        val resp = syncClient.sendPayment(payment)
        when (resp.paymentError.isEmpty()) {
            true -> {
                doSend(WsMsg(MsgType.CONFIRM_PAYMENT_REQ, ConfirmPaymentReq(resp.paymentHash).data2Str()).msg2Str())
            }
            else -> {
                println(resp.paymentError)
            }
        }
    }

    fun doCreateRoom(gameName: String, deposit: Long = 0) {
        Preconditions.checkArgument(deposit >= 0)
        doSend(WsMsg(MsgType.CREATE_ROOM_REQ, CreateRoomReq(gameName, deposit).data2Str()).msg2Str())
    }

    fun doSurrenderReq() {
        val rep = SurrenderReq(roomId).data2Str()
        val msg = WsMsg(MsgType.SURRENDER_REQ, rep).msg2Str()
        doSend(msg)
    }

    fun doChallenge() {
        val rep = ChallengeReq(roomId).data2Str()
        val msg = WsMsg(MsgType.CHALLENGE_REQ, rep).msg2Str()
        doSend(msg)
    }

    private fun doPayment(pr: PaymentReq) {
        val invoice = Invoice(HashUtils.hash160(pr.rhash.decodeBase58()), pr.cost)
        val inviteResp = syncClient.addInvoice(invoice)
        roomId = pr.roomId
        doSend(WsMsg(MsgType.PAYMENT_RESP, PaymentResp(roomId, inviteResp.paymentRequest).data2Str(), from = null, to = roomId).msg2Str())
    }

    private fun doSendPayment(pr: PaymentResp) {
        val payment = Payment(pr.paymentRequest)
        val resp = syncClient.sendPayment(payment)
        when (resp.paymentError.isEmpty()) {
            true -> {
                val psr = PaymentAndStartReq(pr.roomId, resp.paymentHash).data2Str()
                doSend(WsMsg(MsgType.PAYMENT_START_REQ, psr, from = null, to = pr.roomId).msg2Str())
            }
            else -> {
                println(resp.paymentError)
            }
        }
    }

    private fun doPaymentStart(psr: PaymentAndStartReq) {
        var invoice: Invoice
        GlobalScope.launch {
            do {
                invoice = syncClient.lookupInvoice(psr.paymentHash)
            } while (!invoice.invoiceDone())

            if (invoice.state == Invoice.InvoiceState.SETTLED) {
                val psr = PaymentAndStartResp(psr.roomId).data2Str()
                doSend(WsMsg(MsgType.PAYMENT_START_RESP, psr).msg2Str())
            }
        }
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