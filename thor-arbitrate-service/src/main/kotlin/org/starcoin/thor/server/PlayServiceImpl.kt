package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.util.WithLogging
import org.starcoin.thor.core.*
import org.starcoin.thor.core.arbitrate.Arbitrate
import org.starcoin.thor.core.arbitrate.ArbitrateImpl
import org.starcoin.thor.core.arbitrate.ContractImpl
import org.starcoin.thor.core.arbitrate.WitnessContractInput
import org.starcoin.thor.manager.*
import org.starcoin.thor.sign.SignService
import org.starcoin.thor.sign.toByteArray
import java.security.PrivateKey
import java.security.PublicKey

class PlayServiceImpl(private val gameManager: GameManager, private val roomManager: RoomManager) : PlayService {

    companion object : WithLogging()

    private val sessionManager = SessionManager()
    private val commonUserManager = CommonUserManager()
    private val paymentManager = PaymentManager()
    private val arbitrates = mutableMapOf<String, Arbitrate>()
    private val arbitrateLock = java.lang.Object()

    /////Session Data

    override fun sendNonce(sessionId: String, session: DefaultWebSocketSession): String {
        sessionManager.storeSocket(sessionId, session)
        return sessionManager.createNonce(sessionId)
    }

    override fun clearSession(sessionId: String) {
        sessionManager.clearSession(sessionId)
    }

    override fun storePubKey(sessionId: String, userInfo: UserInfo) {
        LOG.info("storePubKey $sessionId, userId: ${userInfo.id}")
        sessionManager.storeUserId(sessionId, userInfo.id)
        commonUserManager.storeUser(userInfo)
    }

    override fun compareNoce(sessionId: String, nonce: String): Boolean {
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

    override fun doCreateRoom(game: String, cost: Long, time: Long, sessionId: String?): Room? {
        Preconditions.checkArgument(cost >= 0 && time >= 0)
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
                val tmp = roomManager.createRoom(gameInfo, cost, time, userId)
                commonUserManager.currentRoom(userId, tmp.roomId)
                tmp

            }
        } else {
            roomManager.createRoom(gameInfo, cost, time)
        }
    }

    override fun doJoinRoom(sessionId: String, roomId: String, arbiter: UserSelf) {
        val currentRoomId = queryUserCurrentRoom(sessionId)
        if (currentRoomId != null) {
            throw RuntimeException("$sessionId has in room $currentRoomId")
        }

        val userId = changeSessionId2UserId(sessionId)
        userId?.let {
            val flag = commonUserManager.normalUserStatus(userId)
            val session = sessionManager.querySocketBySessionId(sessionId)
            if (flag) {
                val room = roomManager.joinRoom(userId, roomId)
                commonUserManager.currentRoom(userId, roomId)
                val resp = JoinRoomResp(true, room)

                session?.let {
                    GlobalScope.launch {
                        session.send(doSign(WsMsg(MsgType.JOIN_ROOM_RESP, arbiter.userInfo.id, resp), arbiter.privateKey))
                    }
                }

                if (room.isFull) {
                    if (!room.payment) {
                        //
                    } else {
                        check(room.cost > 0)
                        doHashs(Pair(room.players[0].playerUserId, room.players[1].playerUserId), roomId, room.cost, arbiter)
                    }
                } else {
                    //nothing
                }
            } else {
                val resp = JoinRoomResp(true)
                session?.let {
                    GlobalScope.launch {
                        session.send(doSign(WsMsg(MsgType.JOIN_ROOM_RESP, arbiter.userInfo.id, resp), arbiter.privateKey))
                    }
                }
            }
        }
    }

    fun doInvoice(sessionId: String, roomId: String, paymentRequest: String, arbiter: UserSelf) {
        //TODO("check the rhash and value of the invoice")
        val userId = changeSessionId2UserId(sessionId)
        val room = roomManager.queryRoomNotNull(roomId)
        if (room.players.map { playerInfo -> playerInfo.playerUserId }.contains(userId)) {
            val other = room.players.map { playerInfo -> playerInfo.playerUserId }.filterNot { userId == it }.first()
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
            val room = roomManager.queryRoomNotNull(roomId)
            room.userReady(userId)
            val flag = room.roomReady()
            if (flag) {
                if (room.payment) {
                    doGameBegin(Pair(room.players[0].playerUserId, room.players[1].playerUserId), roomId, arbiter, false)
                } else {
                    doGameBegin(Pair(room.players[0].playerUserId, room.players[1].playerUserId), roomId, arbiter, true)
                }
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
        surrender(surrender, roomId, arbiter)
    }

    fun doRoomCommonMsg(sessionId: String, roomId: String, msg: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)
        val room = roomManager.queryRoomNotNull(roomId)
        room.players.map { playerInfo -> playerInfo.playerUserId }.filterNot { it != userId }.forEach {
            val session = sessionManager.querySocketByUserId(it)!!
            GlobalScope.launch {
                session.send(doSign(WsMsg(MsgType.ROOM_COMMON_DATA_MSG, arbiter.userInfo.id, CommonRoomData(roomId, msg)), arbiter.privateKey))
            }
        }
    }

    fun doWitness(sessionId: String, data: RoomGameData, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(data.to)
        if (room.players.map { playerInfo -> playerInfo.playerUserId }.contains(userId)) {
            //query pk
            val userInfo = commonUserManager.queryUser(userId)!!
            val pk = userInfo.publicKey

            val signFlag = data.witness.checkSign(pk)

            //check sign and set pk
            if (signFlag) {
                //check arbiter sign
                data.witness.doArbiterSign(arbiter.privateKey)

                //send to room
                room.players.map { playerInfo -> playerInfo.playerUserId }.forEach {
                    val session = sessionManager.querySocketByUserId(it)!!
                    val msg = WsMsg(MsgType.ROOM_GAME_DATA_MSG, arbiter.userInfo.id, data)
                    GlobalScope.launch {
                        session.send(doSign(msg, arbiter.privateKey))
                    }
                }
            }
        }
    }

    fun doChallenge(sessionId: String, roomId: String, witnessList: List<WitnessData>, arbiter: UserSelf) {
        println("do challenge")
        val room = roomManager.queryRoomNotNull(roomId)
        if (room.payment) {
            val userId = changeSessionId2UserId(sessionId)!!
            val detailUser = commonUserManager.queryDetailUser(userId)!!
            if (detailUser.stat == UserStatus.PLAYING && detailUser.currentRoomId == roomId) {
                val userIndex = roomManager.queryUserIndex(roomId, userId)
                check(userIndex > 0)
                val arbitrate = synchronized(arbitrateLock) {
                    when (arbitrates.containsKey(roomId)) {
                        true -> arbitrates[roomId]!!
                        false -> ArbitrateImpl(10 * 60 * 1000) { winner ->
                            if (winner > 0) {
                                val winnerUserId = roomManager.queryUserIdByIndex(roomId, winner)
                                val playerUserId = paymentManager.queryPlayer(winnerUserId, roomId)!!
                                surrender(playerUserId, roomId, arbiter)
                            }
                        }
                    }
                }
                val join = arbitrate.join(userIndex, ContractImpl("http://localhost:3000", "$roomId:$userIndex"))
                if (join) {
                    val otherUser = paymentManager.queryPlayer(userId, roomId)!!
                    val otherUserInfo = commonUserManager.queryUser(otherUser)!!
                    val publicKeys = when (userIndex) {
                        1 -> Triple(arbiter.userInfo.publicKey, detailUser.userInfo.publicKey, otherUserInfo.publicKey)
                        else -> Triple(arbiter.userInfo.publicKey, otherUserInfo.publicKey, detailUser.userInfo.publicKey)
                    }
                    val input = WitnessContractInput(userIndex, publicKeys, witnessList)
                    arbitrate.challenge(input)
                }
            }
        }
    }

    //////private

    private fun doGameEnd(roomId: String) {
        //change user state
        val room = roomManager.queryRoomNotNull(roomId)
        room.players.map { playerInfo -> playerInfo.playerUserId }.forEach {
            commonUserManager.clearRoom(it)
        }
        //clear room info
        roomManager.clearRoom(roomId)
        synchronized(arbitrateLock) {
            arbitrates.remove(roomId)
        }
    }

    private fun doGameBegin(members: Pair<String, String>, roomId: String, arbiter: UserSelf, free: Boolean) {
        val us1 = sessionManager.querySocketByUserId(members.first)
        val us2 = sessionManager.querySocketByUserId(members.second)

        if (us1 != null && us2 != null) {
            val beginTime = System.currentTimeMillis()
            val room = roomManager.queryRoomNotNull(roomId)
            roomManager.roomBegin(roomId, beginTime)
            val begin = when (free) {
                true -> {
                    BeginMsg(room, beginTime)
                }
                false -> {
                    val keys = ArrayList<ByteArrayWrapper>(2)
                    val mk1 = commonUserManager.queryUser(members.first)
                    val mk2 = commonUserManager.queryUser(members.second)
                    keys.add(ByteArrayWrapper(mk1!!.publicKey.toByteArray()))
                    keys.add(ByteArrayWrapper(mk2!!.publicKey.toByteArray()))
                    BeginMsg(room, System.currentTimeMillis(), keys)
                }
            }
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

    private fun surrender(surrenderUserId: String, roomId: String, arbiter: UserSelf) {
        println("do Surrender")
        val playerUserId = paymentManager.queryPlayer(surrenderUserId, roomId)
        playerUserId?.let {
            val r = paymentManager.surrenderR(surrenderUserId, roomId)
            r?.let {
                val session = sessionManager.querySocketByUserId(playerUserId)!!
                doGameEnd(roomId)
                GlobalScope.launch {
                    session.send(doSign(WsMsg(MsgType.SURRENDER_RESP, arbiter.userInfo.id, SurrenderResp(r)), arbiter.privateKey))
                }
            }
        }
    }
}
