package org.starcoin.thor.server

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
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import org.starcoin.lightning.client.core.Invoice
import org.starcoin.lightning.client.core.Payment
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.*
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.toByteArray
import java.io.InputStream
import java.lang.Thread.sleep
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*
import java.util.concurrent.Executors

data class LnConfig(val cert: InputStream, val host: String, val port: Int, val macarron: String)

data class ClientUser(val self: UserSelf, val lnConfig: LnConfig)

private const val HOST = "127.0.0.1"
private const val PORT = 8082
private const val POST_PATH = "/p"
private const val WS_PATH = "/ws"

@UseExperimental(ObsoleteCoroutinesApi::class, KtorExperimentalAPI::class)
class MsgClientServiceImpl(val clientUser: ClientUser) {

    private lateinit var session: ClientWebSocketSession
    private lateinit var roomId: String
    private val rSet = Collections.synchronizedSet(mutableSetOf<ByteArray>())

    private lateinit var chan: Channel
    private lateinit var syncClient: SyncClient

    private lateinit var client: HttpClient

    private val msgChannel = kotlinx.coroutines.channels.Channel<String>(10)
    private lateinit var arbiterPubKey: PublicKey
    private lateinit var otherPubKey: PublicKey

    private val executor = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "payment-thread")
    }

    fun start() {
        // lightning network channel
        chan = Utils.buildChannel(clientUser.lnConfig.cert, clientUser.lnConfig.macarron, clientUser.lnConfig.host, clientUser.lnConfig.port)
        syncClient = SyncClient(chan)

        client = HttpClient(CIO).config {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
            install(WebSockets)
        }

        val pkr = queryPubKey()
        arbiterPubKey = SignService.toPubKey(pkr.pubKey!!.bytes)
        GlobalScope.launch {
            client.ws(method = HttpMethod.Get, host = HOST, port = PORT, path = WS_PATH) {
                session = this

                for (message in incoming.map { it as? Frame.Text }.filterNotNull()) {
                    val msg = message.readText()
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
                doSignAndSend(MsgType.NONCE, Nonce(nr.nonce, ByteArrayWrapper(clientUser.self.userInfo.publicKey.toByteArray())))
            }
            MsgType.CREATE_ROOM_RESP -> {
                val crr = msg.data as CreateRoomResp
                GlobalScope.launch {
                    msgChannel.send(crr.room!!.toJson())
                }
            }
            MsgType.JOIN_ROOM_RESP -> {
                val jrr = msg.data as JoinRoomResp
                GlobalScope.launch {
                    msgChannel.send(jrr.room!!.toJson())
                }
            }
            MsgType.HASH_DATA -> {
                val hr = msg.data as HashData
                doHash(hr)
            }
            MsgType.INVOICE_DATA -> {
                val ir = msg.data as InvoiceData
                payInvoice(ir.paymentRequest)
            }
            MsgType.GAME_BEGIN -> {
                println("todo : game begin")
                val req = msg.data as BeginMsg
                req.keys?.let {
                    val pk = req.keys!!.filterNot { SignService.toPubKey(it.bytes) == clientUser.self.userInfo.publicKey }[0]
                    otherPubKey = SignService.toPubKey(pk.bytes)

                }
            }
            MsgType.SURRENDER_RESP -> {
                val sr = msg.data as SurrenderResp
                if (!sr.winner.isNullOrEmpty()) {
                    if (sr.winner.equals(clientUser.self.userInfo.id)) {
                        println("i win the game !!!")
                        sr.r?.let { rSet.add(sr.r!!.bytes) }
                    } else {
                        println("i lose the game !!!")
                    }
                } else {
                    println("tie!!")
                }
            }
            MsgType.ROOM_COMMON_DATA_MSG -> {
                println("i get the msg: ${msg.data}")
            }
            MsgType.ROOM_GAME_DATA_MSG -> {
                //check sign
                val req = msg.data as RoomGameData
                doRoomGameDataResp(req, otherPubKey)
            }
            MsgType.GAME_END -> {
                println("game end")
            }
            MsgType.CHALLENGE_REQ -> {
                println("i get a challenge msg")
            }
            MsgType.ERR -> {
                val req = msg.data as ErrMsg
                println("err: ${req.code} : ${req.err}")
            }
            else -> {

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

    fun queryGameList(): GameListResp? {
        return queryGameList(1)
    }

    private fun queryGameList(page: Int): GameListResp? {
        var games = GameListResp(0, null)
        runBlocking {
            games = client.post(host = HOST, port = PORT, path = POST_PATH, body = doBody(HttpType.GAME_LIST, GameListReq(page)))
        }

        return games
    }

    fun createRoom(gameId: String, roomName: String, cost: Long = 0) {
        Preconditions.checkArgument(cost >= 0)
        runBlocking {
            doSignAndSend(MsgType.CREATE_ROOM_REQ, CreateRoomReq(gameId, roomName, cost))
        }
    }

    fun queryRoomList(gameName: String): RoomListByGameResp? {
        var resp = RoomListByGameResp(null)
        runBlocking {
            resp = client.post(host = HOST, port = PORT, path = POST_PATH, body = doBody(HttpType.ROOM_LIST, RoomListByGameReq(gameName)))
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
        return SignService.doVerify(signMsg, arbiterPubKey)
    }

    fun roomMsg(roomId: String, msg: String) {
        doSignAndSend(MsgType.ROOM_COMMON_DATA_MSG, CommonRoomData(roomId, msg))
    }

    fun doCreateRoom(gameHash: String, gameName: String, cost: Long = 0) {
        Preconditions.checkArgument(cost >= 0)
        doSignAndSend(MsgType.CREATE_ROOM_REQ, CreateRoomReq(gameHash, gameName, cost))
    }

    fun doSurrenderReq(Id: String) {
        doSignAndSend(MsgType.SURRENDER_REQ, SurrenderReq(Id))
    }

    fun doChallenge(witnessList: List<WitnessData>) {
        doSignAndSend(MsgType.CHALLENGE_REQ, ChallengeReq(roomId, witnessList))
    }

    fun doLeaveRoom(roomId: String) {
        doSignAndSend(MsgType.LEAVE_ROOM, LeaveRoom(roomId))
    }

    private fun doHash(pr: HashData) {
        val invoice = Invoice(pr.rHash.bytes, pr.cost)
        val inviteResp = syncClient.addInvoice(invoice)
        roomId = pr.roomId
        doSignAndSend(MsgType.INVOICE_DATA, InvoiceData(roomId, inviteResp.paymentRequest))
        GlobalScope.launch {
            msgChannel.send(inviteResp.paymentRequest)
        }
    }

    private fun payInvoice(paymentRequest: String) {
        val payment = Payment(paymentRequest)
        executor.submit { syncClient.sendPayment(payment) }
    }

    fun doReady(roomId: String, free: Boolean) {
        if (free) {
            doSignAndSend(MsgType.READY_REQ, ReadyReq(roomId))
        } else {
            val myInvoice = channelMsg()
            val payReq = syncClient.decodePayReq(myInvoice)
            var invoice: Invoice
            do {
                invoice = syncClient.lookupInvoice(payReq.paymentHash)
                sleep(1000)
            } while (!invoice.invoiceDone() && invoice.state != Invoice.InvoiceState.ACCEPTED)
            GlobalScope.launch {
                if (invoice.state == Invoice.InvoiceState.ACCEPTED) {
                    doSignAndSend(MsgType.READY_REQ, ReadyReq(roomId))
                }
            }
        }
    }

    fun doRoomGameDataReq(roomId: String, data: WitnessData) {
        data.doSign(clientUser.self.privateKey)

        doSignAndSend(MsgType.ROOM_GAME_DATA_MSG, RoomGameData(roomId, data))
    }

    private fun doRoomGameDataResp(req: RoomGameData, pk: PublicKey) {
        //check arbiter
        val arbiterFlag = req.witness.checkArbiterSign(arbiterPubKey)

        if (arbiterFlag) {
            var signFlag = req.witness.checkSign(pk)
            if (signFlag) {
                //store data
                println("witness")
            } else {
                signFlag = req.witness.checkSign(clientUser.self.userInfo.publicKey)
                if (signFlag) {
                    //myself, store data
                    println("my witness")
                } else {
                    //error
                }
            }
        } else {
            //error
        }
    }

    fun channelMsg(): String {
        return runBlocking {
            msgChannel.receive()
        }
    }

    fun hasR(): Boolean {
        return rSet.size > 0
    }

    fun priKey(): PrivateKey {
        return clientUser.self.privateKey
    }

    fun stop() {
        client.close()
    }
}