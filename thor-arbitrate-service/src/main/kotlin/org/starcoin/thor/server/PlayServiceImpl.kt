package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.util.ByteUtil
import org.starcoin.thor.core.*
import org.starcoin.thor.manager.*
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.doSign
import java.nio.ByteBuffer
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

    override fun doJoinRoom(sessionId: String, roomId: String, arbiter: UserSelf) {
        val currentRoomId = queryUserCurrentRoom(sessionId)
        if (currentRoomId != null) {
            throw RuntimeException("$sessionId has in room $currentRoomId")
        }

        val userId = changeSessionId2UserId(sessionId)
        userId?.let {
            val room = roomManager.joinRoom(userId, roomId)
            val resp = JoinRoomResp(true, room)
            val session = sessionManager.querySocketBySessionId(sessionId)
            session?.let {
                GlobalScope.launch {
                    session.send(doSign(WsMsg(MsgType.JOIN_ROOM_RESP, arbiter.userInfo.id, resp), arbiter.privateKey))
                }
            }

            if (room.isFull) {
                if (!room.payment) {
                    doGameBegin(Pair(room.players[0], room.players[1]), roomId, arbiter)
                } else {
                    check(room.cost > 0)
                    doHashs(Pair(room.players[0], room.players[1]), roomId, room.cost, arbiter)
                }
            }
        }
    }

    fun doInvoice(sessionId: String, roomId: String, paymentRequest: String, arbiter: UserSelf) {
        //TODO("check the rhash and value of the invoice")
        val userId = changeSessionId2UserId(sessionId)
        val room = roomManager.queryRoomNotNull(roomId)
        if (room.players.contains(userId)) {
            val other = room.players.filterNot { userId == it }.first()
            val otherSession = sessionManager.querySocketByUserId(other)
            val msg = WsMsg(MsgType.INVOICE_REQ, arbiter.userInfo.id, InvoiceReq(roomId, paymentRequest, room.cost))
            GlobalScope.launch {
                otherSession!!.send(doSign(msg, arbiter.privateKey))
            }
        }
    }

    fun doReady(sessionId: String, roomId: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)
        userId?.let {
            paymentManager.userReady(userId, roomId)
            val flag = paymentManager.roomReady(roomId)
            if (flag) {
                val room = roomManager.queryRoomNotNull(roomId)

                doGameBegin(Pair(room.players[0], room.players[1]), roomId, arbiter)
            } else {
                val session = sessionManager.querySocketByUserId(userId)
                session?.let {
                    GlobalScope.launch {
                        session.send(doSign(WsMsg(MsgType.READY_RESP, arbiter.userInfo.id, ReadyResp()), arbiter.privateKey))
                    }
                }
            }

        }
    }

    fun doSurrender(sessionId: String, roomId: String, arbiter: UserSelf) {
        println("do Surrender")
        val surrender = changeSessionId2UserId(sessionId)!!
        val playerUserId = paymentManager.queryPlayer(surrender, roomId)
        playerUserId?.let {
            val r = paymentManager.surrenderR(surrender, roomId)
            r?.let {
                val session = sessionManager.querySocketByUserId(playerUserId)!!
                doGameEnd(roomId)
                GlobalScope.launch {
                    session.send(doSign(WsMsg(MsgType.SURRENDER_RESP, arbiter.userInfo.id, SurrenderResp(r)), arbiter.privateKey))
                }
            }
        }
    }

    fun doRoomCommonMsg(sessionId: String, roomId: String, msg: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)
        val room = roomManager.queryRoomNotNull(roomId)
        room.players.filterNot { it != userId }.forEach {
            val session = sessionManager.querySocketByUserId(it)!!
            GlobalScope.launch {
                session.send(doSign(WsMsg(MsgType.ROOM_COMMON_DATA_MSG, arbiter.userInfo.id, CommonRoomData(roomId, msg)), arbiter.privateKey))
            }
        }
    }

    fun doWitness(sessionId: String, data: RoomGameData, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(data.to)
        if (room.players.contains(userId)) {
            println("-------11111111---------")
            //find user num in room
            val flag = (room.players[0] == userId)
            //query pk
            val userInfo = commonUserManager.queryUser(userId)!!
            val pk = userInfo.publicKey

            var signFlag = when (flag) {
                true -> data.witness.checkFirstSign(pk)
                else -> data.witness.checkSecondSign(pk)
            }

            //check sign and set pk
            if (signFlag) {
                println("-------2222222---------")

                when (flag) {
                    true -> data.firstPlayerPk = ByteArrayWrapper(pk.encoded)
                    false -> data.secondPlayerPk = ByteArrayWrapper(pk.encoded)
                }

                //check arbiter sign
                if (data.witness.arbiterSign == null && data.witness.timestamp == null) {
                    println("-------3333333---------")
                    data.witness.doArbiterSign(arbiter.privateKey)
                } else {
                    println("-------4444444---------")
                    signFlag = data.witness.checkArbiterSign(arbiter.userInfo.publicKey)
                }

                //send to room
                if (signFlag) {
                    println("-------555555---------")
                    room.players.forEach {
                        val session = sessionManager.querySocketByUserId(it)!!
                        val msg = WsMsg(MsgType.ROOM_GAME_DATA_MSG, arbiter.userInfo.id, data)
                        GlobalScope.launch {
                            session.send(doSign(msg, arbiter.privateKey))
                        }
                    }
                }
            }
        }
    }
    //////private

    private fun doGameEnd(roomId: String) {
        //change user state
        val room = roomManager.queryRoomNotNull(roomId)
        room.players.forEach {
            commonUserManager.clearRoom(it)
        }
        //clear room info
        roomManager.clearRoom(roomId)
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

    private fun doHashs(members: Pair<String, String>, roomId: String, cost: Long, arbiter: UserSelf) {
        val us1 = sessionManager.querySocketByUserId(members.first)
        val us2 = sessionManager.querySocketByUserId(members.second)
        if (us1 != null && us2 != null) {
            val payPair = paymentManager.generatePayments(roomId, members.first, members.second)
            val msg1 = WsMsg(MsgType.HASH_REQ, arbiter.userInfo.id, HashReq(roomId, payPair.first.rHash, cost))
            val msg2 = WsMsg(MsgType.HASH_REQ, arbiter.userInfo.id, HashReq(roomId, payPair.second.rHash, cost))
            GlobalScope.launch {
                us1.send(doSign(msg1, arbiter.privateKey))
                us2.send(doSign(msg2, arbiter.privateKey))
            }
        }
    }

    private fun queryUserCurrentRoom(sessionId: String): String? {
        val userId = changeSessionId2UserId(sessionId)
        return userId?.let { commonUserManager.queryCurrentRoom(userId) }
    }

    override fun doSign(msg: WsMsg, priKey: PrivateKey): Frame.Text {
        return Frame.Text(SignService.doSign(msg, priKey).toJson())
    }
}