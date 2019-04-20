package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.thor.core.*
import org.starcoin.thor.manager.*
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.doSign
import java.security.PrivateKey
import java.security.PublicKey

class PlayServiceImpl(private val gameManager: GameManager, private val roomManager: RoomManager) : PlayService {

    private val sessionManager = SessionManager()
    private val commonUserManager = CommonUserManager()
    private val paymentManager = PaymentManager()

    /////Session Data

    override fun sendNonce(sessionId: String, session: DefaultWebSocketSession): Long {
        sessionManager.storeSocket(sessionId, session)
        return sessionManager.createNonce(sessionId)
    }

    override fun clearSession(sessionId: String) {
        sessionManager.clearSession(sessionId)
    }

    override fun storePubKey(sessionId: String, userInfo: UserInfo) {
        sessionManager.storeUserId(sessionId, userInfo.id)
        commonUserManager.storeUser(userInfo)
    }

    override fun compareNoce(sessionId: String, nonce: Long): Boolean {
        return nonce == sessionManager.queryNonce(sessionId)
    }

    private fun changeSessionId2UserId(sessionId: String): String? {
        return sessionManager.queryUserIdBySessionId(sessionId)
    }

    //////User Data

    override fun queryPubKey(sessionId: String): PublicKey? {
        val userId = changeSessionId2UserId(sessionId)
        return userId?.let {
            val userInfo = commonUserManager.queryUser(userId)
            userInfo?.let { userInfo.publicKey }
        }
    }

    override fun doCreateRoom(game: String, deposit: Long, time: Long, sessionId: String?): Room? {
        Preconditions.checkArgument(deposit >= 0 && time >= 0)
        val gameInfo = gameManager.queryGameBaseInfoByHash(game)
                ?: throw NotFoundException("Can not find game by hash: $game")

        var flag = false
        sessionId?.let {
            val currentRoomId = queryUserCurrentRoom(sessionId)
            check(currentRoomId == null)
            flag = true
        }

        return if (flag) {
            val userId = changeSessionId2UserId(sessionId!!)
            userId?.let {
                val tmp = roomManager.createRoom(gameInfo, deposit, time, userId)
                commonUserManager.setCurrentRoom(userId, tmp.roomId)
                tmp

            }
        } else {
            roomManager.createRoom(gameInfo, deposit, time)
        }
    }

    private fun queryUserCurrentRoom(sessionId: String): String? {
        val userId = changeSessionId2UserId(sessionId)
        return userId?.let { commonUserManager.queryCurrentRoom(userId) }
    }

    override fun doJoinRoom(sessionId: String, roomId: String, arbiter: UserSelf) {
        val currentRoomId = queryUserCurrentRoom(sessionId)
        if (currentRoomId != null) {
            throw RuntimeException("$sessionId has in room $currentRoomId")
        }

        val userId = changeSessionId2UserId(sessionId)
        userId?.let {
            val room = roomManager.joinRoom(userId, roomId)
            if (room.isFull) {
                if (!room.payment) {
                    doGameBegin(Pair(room.players[0], room.players[1]), roomId, arbiter)
                } else {
                    check(room.cost > 0)
                    doInvoices(Pair(room.players[0], room.players[1]), roomId, room.cost, arbiter)
                }
            } else {
                val resp = JoinRoomResp(roomId, true)
                val session = sessionManager.querySocketBySessionId(sessionId)
                session?.let {
                    GlobalScope.launch {
                        session.send(doSign(WsMsg(MsgType.JOIN_ROOM_RESP, arbiter.userInfo.id, resp), arbiter.privateKey))
                    }
                }
            }
        }
    }

    private fun doGameBegin(members: Pair<String, String>, roomId: String, arbiter: UserSelf) {
        val us1 = sessionManager.querySocketByUserId(members.first)
        val us2 = sessionManager.querySocketByUserId(members.second)

        if (us1 != null && us2 != null) {
            val room = roomManager.queryRoomNotNull(roomId)
            val begin = BeginMsg(room)
            val msg1 = WsMsg(MsgType.GAME_BEGIN, arbiter.userInfo.id, begin)
            val msg2 = WsMsg(MsgType.GAME_BEGIN, arbiter.userInfo.id, begin)
            val us = Pair(members.first, members.second)
            commonUserManager.gameBegin(us)
            GlobalScope.launch {
                us1.send(doSign(msg1, arbiter.privateKey))
                us2.send(doSign(msg2, arbiter.privateKey))
            }
        }
    }

    private fun doInvoices(members: Pair<String, String>, roomId: String, cost: Long, arbiter: UserSelf) {
        val us1 = sessionManager.querySocketByUserId(members.first)
        val us2 = sessionManager.querySocketByUserId(members.second)
        if (us1 != null && us2 != null) {
            val payPair = paymentManager.generatePayments(roomId, members.first, members.second)
            val msg1 = WsMsg(MsgType.INVOICE_REQ, arbiter.userInfo.id, InvoiceReq(roomId, payPair.first.rHash, cost))
            val msg2 = WsMsg(MsgType.INVOICE_REQ, arbiter.userInfo.id, InvoiceReq(roomId, payPair.second.rHash, cost))
            GlobalScope.launch {
                us1.send(doSign(msg1, arbiter.privateKey))
                us2.send(doSign(msg2, arbiter.privateKey))
            }
        }
    }

    override fun doSign(msg: WsMsg, priKey: PrivateKey): Frame.Text {
        return Frame.Text(SignService.doSign(msg, priKey).toJson())
    }
}