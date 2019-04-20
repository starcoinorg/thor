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
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.*
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.doSign
import org.starcoin.thor.sign.doVerify
import org.starcoin.thor.utils.decodeBase58
import java.io.InputStream
import java.security.PublicKey
import java.util.*

data class LnConfig(val cert: InputStream, val host: String, val port: Int)

data class ClientUser(val self: UserSelf, val lnConfig: LnConfig)

private const val HOST = "127.0.0.1"
private const val PORT = 8082
private const val POST_PATH = "/p"
private const val WS_PATH = "/ws"

class MsgClientServiceImpl(private val clientUser: ClientUser) {

    private lateinit var session: ClientWebSocketSession
    private lateinit var roomId: String
    private val rSet = Collections.synchronizedSet(mutableSetOf<String>())

    private lateinit var chan: Channel
    private lateinit var syncClient: SyncClient

    private lateinit var client: HttpClient

    private val msgChannel = kotlinx.coroutines.channels.Channel<String>(10)
    private lateinit var sessionId: String
    private lateinit var pubKey: PublicKey

    fun start() {
        // lightning network channel
        chan = Utils.buildChannel(clientUser.lnConfig.cert, clientUser.lnConfig.host, clientUser.lnConfig.port)
        syncClient = SyncClient(chan)

        client = HttpClient(CIO).config {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(WebSockets)
        }

        val pkr = queryPubKey()
        pubKey = SignService.toPubKey(pkr.pubKey!!.bytes)
        GlobalScope.launch {
            client.ws(method = HttpMethod.Get, host = HOST, port = PORT, path = WS_PATH) {
                session = this

                for (message in incoming.map { it as? Frame.Text }.filterNotNull()) {
                    val msg = message.readText()
                    println(msg)
                    val resp = MsgObject.fromJson(msg, SignMsg::class)
                    if (doVerify(resp)) {
                        doMsg(resp.msg)
                    } else {
                        //do nothing
                    }
                }
            }
        }
    }

    private fun doMsg(msg: WsMsg) {
        when (msg.type) {
            MsgType.NONCE -> {
                val nr = msg.data as Nonce
                doSignAndSend(MsgType.NONCE, Nonce(nr.nonce, ByteArrayWrapper(clientUser.self.userInfo.publicKey.encoded)))
            }
            MsgType.CREATE_ROOM_RESP -> {
                val crr = msg.data as CreateRoomResp
                GlobalScope.launch {
                    msgChannel.send(crr.roomId!!)
                }
            }
            MsgType.PAYMENT_REQ -> {
                val pr = msg.data as PaymentReq
                doPayment(pr)
            }
            MsgType.PAYMENT_RESP -> {
                val pr = msg.data as PaymentResp
                doSendPayment(pr)
            }
            MsgType.PAYMENT_START_REQ -> {
                val psr = msg.data as PaymentAndStartReq
                doPaymentStart(psr)
            }
            MsgType.JOIN_ROOM_RESP -> {
                //TODO
            }
            MsgType.GAME_BEGIN -> {
                println("todo : game begin")
                //TODO("game begin")
            }
            MsgType.SURRENDER_RESP -> {
                val sr = msg.data as SurrenderResp
                println("i win the game !!!")
                rSet.add(sr.r)
            }
            MsgType.ROOM_DATA_MSG -> {
                println("i get the ${msg.userId} msg: ${msg.data}")
            }
        }
    }

    /////HTTP

    private fun queryPubKey(): PubKeyResp {
        var pubkey = PubKeyResp(null)

        runBlocking {
            pubkey = client.post(host = HOST, port = PORT, path = POST_PATH, body = doBody(HttpType.PUB_KEY, PubKeyReq()))
        }
        return pubkey
    }

    private fun doBody(type: HttpType, data: Data): TextContent {
        return TextContent(HttpMsg(type, data).toJson(), ContentType.Application.Json)
    }

    fun joinRoom(roomId: String) {
        doSignAndSend(MsgType.JOIN_ROOM_REQ, JoinRoomReq(roomId))
    }

    fun createGame(): String {
        val defaultGame = "test-" + Random().nextLong()
        println(defaultGame)
        createGame(defaultGame)
        return defaultGame
    }

    fun createGame(gameName: String) {
        runBlocking {
            client.post<String>(host = HOST, port = PORT, path = POST_PATH, body = TextContent(HttpMsg(HttpType.CREATE_GAME, CreateGameReq(gameName, ByteArrayWrapper(gameName.toByteArray()), 2)).toJson(), ContentType.Application.Json))
        }
    }

    fun queryGameList(): GameListResp? {
        return queryGameList(1)
    }

    fun queryGameList(page: Int): GameListResp? {
        var games = GameListResp(0, null)
        runBlocking {
            games = client.post(host = HOST, port = PORT, path = POST_PATH, body = doBody(HttpType.GAME_LIST, GameListReq(page)))
        }

        return games
    }

    fun createRoom(gameName: String, deposit: Long = 0): CreateRoomResp {
        Preconditions.checkArgument(deposit >= 0)
        var resp = CreateRoomResp(null)
        runBlocking {
            resp = client.post(host = HOST, port = PORT, path = POST_PATH, body = doBody(HttpType.CREATE_ROOM, CreateRoomReq(gameName, deposit)))
        }

        return resp
    }

    fun queryRoomList(gameName: String): RoomListResp? {
        var resp = RoomListResp(null)
        runBlocking {
            resp = client.post(host = HOST, port = PORT, path = POST_PATH, body = doBody(HttpType.ROOM_LIST, RoomListReq(gameName)))
        }

        return resp
    }

    /////WS

    private fun doSignAndSend(type: MsgType, data: Data) {
        val msg = WsMsg(type, clientUser.self.userInfo.id, data)
        val text = Frame.Text(SignService.doSign(msg, this.clientUser.self.privateKey).toJson())
        GlobalScope.launch { session.send(text) }
    }

    private fun doVerify(signMsg: SignMsg): Boolean {
        return SignService.doVerify(signMsg, pubKey)
    }

    fun roomMsg(roomId: String, msg: String) {
        doSignAndSend(MsgType.ROOM_DATA_MSG, RoomData(roomId, ByteArrayWrapper(msg.toByteArray())))
    }

    fun doCreateRoom(gameName: String, deposit: Long = 0) {
        Preconditions.checkArgument(deposit >= 0)
        doSignAndSend(MsgType.CREATE_ROOM_REQ, CreateRoomReq(gameName, deposit))
    }

    fun doSurrenderReq() {
        doSignAndSend(MsgType.SURRENDER_REQ, SurrenderReq(roomId))
    }

    fun doChallenge() {
        doSignAndSend(MsgType.CHALLENGE_REQ, ChallengeReq(roomId))
    }

    private fun doPayment(pr: PaymentReq) {
        val invoice = Invoice(HashUtils.hash160(pr.rhash.decodeBase58()), pr.cost)
        val inviteResp = syncClient.addInvoice(invoice)
        roomId = pr.roomId
        doSignAndSend(MsgType.PAYMENT_RESP, PaymentResp(roomId, inviteResp.paymentRequest))
    }

    private fun doSendPayment(pr: PaymentResp) {
        val payment = Payment(pr.paymentRequest)
        val resp = syncClient.sendPayment(payment)
        when (resp.paymentError.isEmpty()) {
            true -> {
                val psr = PaymentAndStartReq(pr.roomId, resp.paymentHash)
                doSignAndSend(MsgType.PAYMENT_START_REQ, psr)
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
                val psr = PaymentAndStartResp(psr.roomId)
                doSignAndSend(MsgType.PAYMENT_START_RESP, psr)
            }
        }
    }

    fun channelMsg(): String {
        val msg = runBlocking {
            msgChannel.receive()
        }

        return msg
    }
}