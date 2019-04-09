package org.starcoin.thor.server

import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.thor.core.*

class MsgServiceImpl(private val userManager: UserManager, private val gameManager: GameManager) {

    private val paymentManager = PaymentManager()

    fun doConnection(fromAddr: String, connData: ConnData, session: DefaultWebSocketSession): Boolean {
        return userManager.addUser(User(session, fromAddr, UserStatus.CONNECTED))
        //TODO("return CONFIRM_REQ")
    }

    fun doStartAndInvite(gameHash: String, fromAddr: String, toAddr: String): StartAndInviteResp {
        //TODO("remove UserStatus.CONNECTED")
        val fromU = userManager.queryUser(fromAddr)
        val toU = userManager.queryUser(toAddr)
        if ((fromU!!.stat == UserStatus.CONNECTED || fromU.stat == UserStatus.CONFIRMED)
                && (toU!!.stat == UserStatus.CONNECTED || toU.stat == UserStatus.CONFIRMED)) {
            val game = gameManager.queryGameByHash(gameHash)
            game?.let {
                val id = gameManager.generateInstance(gameHash)
                id?.let {
                    val pair = paymentManager.generatePayments(gameHash, fromAddr, toAddr)
                    val iap = InvitedAndPaymentReq(gameHash, id, pair.second.rHash).data2Str()
                    GlobalScope.launch { toU.session.send(Frame.Text(WsMsg(fromAddr, toAddr, MsgType.INVITE_PAYMENT_REQ, iap).msg2Str())) }
                    return StartAndInviteResp(true, InvitedAndPaymentReq(gameHash, id, pair.first.rHash))
                }
            }
        }

        return StartAndInviteResp(false)
    }

    fun doSurrender(surrenderAddr: String, instanceId: String) {
        val r = paymentManager.surrenderR(surrenderAddr, instanceId)
        r?.let {
            val toU = userManager.queryUser(r.second)
            toU?.let {
                val resp = SurrenderResp(r.first).data2Str()
                toU?.let {
                    GlobalScope.launch { toU.session.send(Frame.Text(WsMsg(surrenderAddr, r.second, MsgType.SURRENDER_RESP, resp).msg2Str())) }
                }
            }
        }
    }

    fun doChallenge(surrenderAddr: String, instanceId: String) {
        //TODO()
    }

    fun doOther(msg: WsMsg) {
        val toU = userManager.queryUser(msg.to)
        toU?.let {
            GlobalScope.launch { toU.session.send(Frame.Text(msg.msg2Str())) }
        }
    }

}