package org.starcoin.thor.server

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.thor.core.*

class MsgServiceImpl(private val userManager: UserManager, private val gameManager: GameManager) {

    private val paymentManager = PaymentManager()

    fun doConnection(newUser: User) {
        newUser.stat = UserStatus.CONFIRMED
        userManager.addUser(newUser)
        //TODO("return CONFIRM_REQ and use addUser()")
    }

    fun doStartAndInvite(gameHash: String, fromAddr: String, toAddr: String): StartAndInviteResp {
        val fromU = userManager.queryUser(fromAddr)!!
        val toU = userManager.queryUser(toAddr)!!
        if (fromU.stat == UserStatus.CONFIRMED
                && toU.stat == UserStatus.CONFIRMED) {
            val game = gameManager.queryGameByHash(gameHash)
            game?.let {
                val id = gameManager.generateInstance(gameHash)
                id?.let {
                    val pair = paymentManager.generatePayments(id, fromAddr, toAddr)
                    val iap = InvitedAndPaymentReq(gameHash, id, pair.second.rHash).data2Str()
                    GlobalScope.launch { toU.session.send(Frame.Text(WsMsg(fromAddr, toAddr, MsgType.INVITE_PAYMENT_REQ, iap).msg2Str())) }
                    return StartAndInviteResp(true, InvitedAndPaymentReq(gameHash, id, pair.first.rHash))
                }
            }
        }

        return StartAndInviteResp(false)
    }

    fun doGameBegin(addr: String, adminAddr: String, psr: PaymentAndStartResp) {
        val pair = paymentManager.changePaymentStatus(addr, psr.instanceId)
        pair?.let {
            if (pair.first.received && pair.second.received) {
                val u1 = userManager.queryUser(pair.first.addr)!!
                val u2 = userManager.queryUser(pair.second.addr)!!
                val begin = BeginMsg(psr.instanceId).data2Str()
                val msg1 = WsMsg(adminAddr, u1.sessionId!!, MsgType.GAME_BEGIN, begin)
                val msg2 = WsMsg(adminAddr, u2.sessionId!!, MsgType.GAME_BEGIN, begin)
                var us = Pair(u1.sessionId!!, u2.sessionId!!)
                val flag = userManager.gameBegin(us)
                if (flag) {
                    GlobalScope.launch {
                        u1.session.send((Frame.Text(msg1.msg2Str())))
                        u2.session.send((Frame.Text(msg2.msg2Str())))
                    }
                }
            }
        }
    }

    fun doSurrender(surrenderAddr: String, instanceId: String) {
        println("do Surrender")
        val player = paymentManager.queryPlayer(surrenderAddr, instanceId)
        player?.let {
            val toU = userManager.queryUser(player)
            toU?.let {
                val r = paymentManager.surrenderR(surrenderAddr, instanceId)
                r?.let {
                    val us = Pair(surrenderAddr, player)
                    val flag = userManager.gameEnd(us)
                    if (flag) {
                        val resp = SurrenderResp(r).data2Str()
                        GlobalScope.launch { toU.session.send(Frame.Text(WsMsg(surrenderAddr, toU.sessionId!!, MsgType.SURRENDER_RESP, resp).msg2Str())) }
                    }
                }
            }
        }
    }

    fun doChallenge(challengeAddr: String, instanceId: String) {
        //TODO("challenge")
        println("do challenge")
        val player = paymentManager.queryPlayer(challengeAddr, instanceId)
        player?.let {
            val flag = java.util.Random().nextBoolean()
            if (flag)
                doSurrender(challengeAddr, instanceId)
            else
                doSurrender(player, instanceId)
        }
    }

    fun doOther(msg: WsMsg) {
        val toU = userManager.queryUser(msg.to)
        toU?.let {
            GlobalScope.launch { toU.session.send(Frame.Text(msg.msg2Str())) }
        }
    }
}