package org.starcoin.thor.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.WebSocketSession
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.filterNotNull
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.launch
import org.starcoin.lightning.client.HashUtils
import org.starcoin.lightning.client.core.Invoice
import org.starcoin.lightning.client.core.Payment
import org.starcoin.thor.core.*
import org.starcoin.thor.utils.decodeBase58
import java.util.*

class MsgClientServiceImpl(private val lnClient: LnClient) {

    private lateinit var session: WebSocketSession
    private lateinit var gameClient: GameClientServiceImpl
    private lateinit var adminAddr: String
    private lateinit var gameInstanceId: String
    private val rSet = Collections.synchronizedSet(mutableSetOf<String>())
    fun start() {
        gameClient = GameClientServiceImpl()
        gameClient.start()

        adminAddr = gameClient.queryAdmin()

        val client = HttpClient(CIO).config {
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
        when {
            msg.type == MsgType.INVITE_PAYMENT_REQ -> {
                println("=====111")
                println(msg)
//                GlobalScope.launch {
                val ipr = msg.str2Data(InvitedAndPaymentReq::class)
                doInvitedAndPayment(msg.from, ipr)
//                }
            }
            msg.type == MsgType.START_INVITE_RESP -> {
                println("=====222")
                println(msg)
//                GlobalScope.launch {
                val sir = msg.str2Data(StartAndInviteResp::class)
                doStartAndInviteResp(msg.from, sir)
//                }
            }
            msg.type == MsgType.INVITE_PAYMENT_RESP -> {
                println("=====444")
//                GlobalScope.launch {
                val ipr = msg.str2Data(InvitedAndPaymentResp::class)
                doSendPayment(ipr.invoice)
//                }
                println("=====555")
            }
            msg.type == MsgType.SURRENDER_RESP -> {
                val sr = msg.str2Data(SurrenderResp::class)
                rSet.add(sr.r)
            }
        }
    }

    private fun doConnection() {
        val data = ConnData().data2Str()
        val conn = WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.CONN, data).msg2Str()
        doSend(conn)
    }

    fun doStartAndInviteReq(gameHash: String, toAddr: String) {
        val sai = StartAndInviteReq(gameHash).data2Str()
        val msg = WsMsg(lnClient.conf.addr!!, toAddr, MsgType.START_INVITE_REQ, sai).msg2Str()
        doSend(msg)
    }

    fun doSurrenderReq() {
        val rep = SurrenderReq(gameInstanceId).data2Str()
        val msg = WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.SURRENDER_REQ, rep).msg2Str()
        doSend(msg)
    }

    fun doChallenge() {
        val rep = ChallengeReq(gameInstanceId).data2Str()
        val msg = WsMsg(lnClient.conf.addr!!, adminAddr, MsgType.CHALLENGE_REQ, rep).msg2Str()
        doSend(msg)
    }

    private fun doInvitedAndPayment(fromAddr: String, ipr: InvitedAndPaymentReq) {
        val gameInfo = gameClient.queryGame(ipr.gameHash!!.toByteArray())
        gameInfo?.let {
            println("--->" + gameInfo.cost)
            val invoice = Invoice(HashUtils.hash160(ipr.rhash.decodeBase58()), gameInfo.cost)
            val inviteResp = lnClient.syncClient.addInvoice(invoice)
            gameInstanceId = ipr.instanceId!!
            doSend(WsMsg(lnClient.conf.addr!!, fromAddr, MsgType.INVITE_PAYMENT_RESP, InvitedAndPaymentResp(gameInstanceId, inviteResp.paymentRequest).data2Str()).msg2Str())
        }
    }

    private fun doStartAndInviteResp(fromAddr: String, sir: StartAndInviteResp) {
        if (sir.succ) {
            doInvitedAndPayment(fromAddr, sir.iap!!)
        }
    }

    private fun doSendPayment(invoice: String) {
        lnClient.syncClient.sendPayment(Payment(invoice))
    }

    private fun doSend(msg: String) {
        GlobalScope.launch { session.send(Frame.Text(msg)) }
    }
}