package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.sirius.util.WithLogging
import org.starcoin.sirius.util.error
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
    private val arbitrateLock = Object()

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
                ?: throw NotFoundException("Can not find user by sessionId: $sessionId")
        return commonUserManager.queryUser(userId)!!.publicKey
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
            val userId = changeSessionId2UserId(sessionId!!)!!
            val user = commonUserManager.queryUser(userId)!!
            val tmp = roomManager.createRoom(gameInfo, cost, time, user)
            commonUserManager.currentRoom(userId, tmp.roomId)
            tmp
        } else {
            roomManager.createRoom(gameInfo, cost, time)
        }
    }

    override fun doJoinRoom(sessionId: String, roomId: String, arbiter: UserSelf) {
        val currentRoomId = queryUserCurrentRoom(sessionId)
        if (currentRoomId != null) {
            doException("$sessionId has in room $currentRoomId")
        }

        val userId = changeSessionId2UserId(sessionId)!!
        val flag = commonUserManager.normalUserStatus(userId)
        val session = sessionManager.querySocketBySessionId(sessionId)!!
        if (flag) {
            val user = commonUserManager.queryUser(userId)!!
            val room = roomManager.joinRoom(user, roomId)
            commonUserManager.currentRoom(userId, roomId)
            val resp = JoinRoomResp(true, room.deepCopy())

            GlobalScope.launch {
                session.send(doSign(WsMsg(MsgType.JOIN_ROOM_RESP, arbiter.userInfo.id, resp), arbiter.privateKey))
            }

            if (room.isFull) {
                if (!room.payment) {
                    //
                } else {
                    check(room.cost > 0)
                    doHash(Pair(room.players[0].playerUserId, room.players[1].playerUserId), roomId, room.cost, arbiter)
                }
            } else {
                //nothing
            }
        } else {
            val resp = JoinRoomResp(false)
            GlobalScope.launch {
                session.send(doSign(WsMsg(MsgType.JOIN_ROOM_RESP, arbiter.userInfo.id, resp), arbiter.privateKey))
            }
        }
    }

    fun doInvoice(sessionId: String, roomId: String, paymentRequest: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(roomId)
        if (room.isInRoom(userId)) {
//            val invoice = Utils.decode(paymentRequest)!!
//            println("====1===>" + invoice.value)
//            println("====2===>" + room.cost)
//            check(invoice.value == (room.cost * 1000))
//            //TODO("check rhash")
//            val rhash = paymentManager.queryRHash(userId, roomId)!!
//            println("----1->" + invoice.getrHash().toHEXString())
//            println("----2->" + HashUtils.hash160(rhash.decodeBase58()).toHEXString())
//            check(invoice.getrHash().toHEXString() == HashUtils.hash160(rhash.decodeBase58()).toHEXString())

            val other = room.players.map { playerInfo -> playerInfo.playerUserId }.filterNot { userId == it }.first()
            val otherSession = sessionManager.querySocketByUserId(other)!!
            val msg = WsMsg(MsgType.INVOICE_DATA, arbiter.userInfo.id, InvoiceData(roomId, paymentRequest))
            GlobalScope.launch {
                otherSession.send(doSign(msg, arbiter.privateKey))
            }
        } else {
            doException("doInvoice：user $userId is not in $roomId room")
        }
    }

    fun doReady(sessionId: String, roomId: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(roomId)
        val inRoom = room.isInRoom(userId)
        if (inRoom) {
            room.userReady(userId)
            val flag = room.roomReady()
            if (flag) {
                if (room.payment) {
                    doGameBegin(Pair(room.players[0].playerUserId, room.players[1].playerUserId), roomId, arbiter, false)
                } else {
                    doGameBegin(Pair(room.players[0].playerUserId, room.players[1].playerUserId), roomId, arbiter, true)
                }
            } else {
                LOG.info("user $userId ready")
            }
        } else {
            doException("doReady：user $userId is not in $roomId room")
        }
    }

    fun doSurrender(sessionId: String, roomId: String, arbiter: UserSelf) {
        println("do Surrender")
        val surrender = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(roomId)
        val inRoom = room.isInRoom(surrender)
        if (inRoom) {
            surrender(surrender, roomId, arbiter, false, false)
        } else {
            doException("doSurrender: user $surrender is not in $roomId room")
        }
    }

    fun doRoomCommonMsg(sessionId: String, roomId: String, msg: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(roomId)
        val inRoom = room.isInRoom(userId)
        if (inRoom) {
            val room = roomManager.queryRoomNotNull(roomId)
            room.players.map { playerInfo -> playerInfo.playerUserId }.filterNot { it != userId }.forEach {
                val session = sessionManager.querySocketByUserId(it)!!
                GlobalScope.launch {
                    session.send(doSign(WsMsg(MsgType.ROOM_COMMON_DATA_MSG, arbiter.userInfo.id, CommonRoomData(roomId, msg)), arbiter.privateKey))
                }
            }
        } else {
            doException("doRoomCommonMsg: user $userId is not in $roomId room")
        }
    }

    fun doWitness(sessionId: String, data: RoomGameData, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(data.to)
        val inRoom = room.isInRoom(userId)
        if (inRoom) {
            val room = roomManager.queryRoomNotNull(data.to)
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
            } else {
                doException("check witness sign fail for msg ${data.toJson()}")
            }
        } else {
            doException("doWitness: user $userId is not in ${data.to} room")
        }
    }

    fun doLeaveRoom(sessionId: String, roomId: String, arbiter: UserSelf) {
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(roomId)
        val inRoom = room.isInRoom(userId)
        if (inRoom) {
            surrender(userId, roomId, arbiter, true, false)
        } else {
            doException("doChallenge: user $userId is not in roomId room")
        }
    }

    fun doChallenge(sessionId: String, roomId: String, witnessList: List<WitnessData>, arbiter: UserSelf) {
        println("do challenge")
        val userId = changeSessionId2UserId(sessionId)!!
        val room = roomManager.queryRoomNotNull(roomId)
        val gameInfo = gameManager.queryGameInfoByHash(room.gameId)
        val inRoom = room.isInRoom(userId)
        if (inRoom) {
//            val room = roomManager.queryRoomNotNull(roomId)
//            if (room.payment) {
            val detailUser = commonUserManager.queryDetailUser(userId)!!
            if (detailUser.stat == UserStatus.PLAYING && detailUser.currentRoomId == roomId) {
                val userIndex = roomManager.queryUserIndex(roomId, userId)
                check(userIndex > 0)
                val arbitrate = synchronized(arbitrateLock) {
                    when (arbitrates.containsKey(roomId)) {
                        true -> arbitrates[roomId]!!
                        false -> ArbitrateImpl(1 * 60 * 1000) { winner ->
                            if (winner.toInt() > 0) {
                                val winnerUserId = roomManager.queryUserIdByIndex(roomId, winner.toInt())
                                val playerUserId = room.rivalPlayer(winnerUserId)!!
                                surrender(playerUserId, roomId, arbiter, false, false)
                            } else {
                                //TODO("tie")
                            }
                        }
                    }
                }
                val join = arbitrate.join(userIndex.toString(),
                        ContractImpl("http://localhost:3000", "$roomId:$userIndex", gameInfo.engineBytes.bytes))
                if (join) {
                    val otherUser = room.rivalPlayer(userId)!!
                    val otherUserInfo = commonUserManager.queryUser(otherUser)!!
                    val publicKeys = when (userIndex) {
                        1 -> Triple(arbiter.userInfo.publicKey, detailUser.userInfo.publicKey, otherUserInfo.publicKey)
                        else -> Triple(arbiter.userInfo.publicKey, otherUserInfo.publicKey, detailUser.userInfo.publicKey)
                    }
                    val input = WitnessContractInput(userIndex.toString(), publicKeys, witnessList)
                    arbitrate.challenge(input)
                    //send msg
                    val otherSession = sessionManager.querySocketByUserId(otherUser)!!
                    GlobalScope.launch {
                        otherSession.send(doSign(WsMsg(MsgType.CHALLENGE_REQ, arbiter.userInfo.id, ChallengeReq(roomId, witnessList)), arbiter.privateKey))
                    }
                } else {
                    doException("doChallenge: user $userId join arbitrate err")
                }
            } else {
                doException("doChallenge: user $userId status is err")
            }
//            } else {
//                doException("doChallenge: $roomId room is free")
//            }
        } else {
            doException("doChallenge: user $userId is not in roomId room")
        }
    }

//////private

    private fun doGameBegin(members: Pair<String, String>, roomId: String, arbiter: UserSelf, free: Boolean) {
        val us1 = sessionManager.querySocketByUserId(members.first)!!
        val us2 = sessionManager.querySocketByUserId(members.second)!!

        val beginTime = System.currentTimeMillis()
        val room = roomManager.queryRoomNotNull(roomId)
        roomManager.roomBegin(roomId, beginTime)

        val keys = ArrayList<ByteArrayWrapper>(2)
        val mk1 = commonUserManager.queryUser(members.first)!!
        val mk2 = commonUserManager.queryUser(members.second)!!
        keys.add(ByteArrayWrapper(mk1.publicKey.toByteArray()))
        keys.add(ByteArrayWrapper(mk2.publicKey.toByteArray()))
        val begin = BeginMsg(room.deepCopy(), beginTime, keys)

        val msg1 = WsMsg(MsgType.GAME_BEGIN, arbiter.userInfo.id, begin)
        val msg2 = WsMsg(MsgType.GAME_BEGIN, arbiter.userInfo.id, begin)
        val us = Pair(members.first, members.second)
        commonUserManager.gameBegin(us)
        GlobalScope.launch {
            us1.send(doSign(msg1, arbiter.privateKey))
            us2.send(doSign(msg2, arbiter.privateKey))
        }
    }

    private fun doHash(members: Pair<String, String>, roomId: String, cost: Long, arbiter: UserSelf) {
        val us1 = sessionManager.querySocketByUserId(members.first)!!
        val us2 = sessionManager.querySocketByUserId(members.second)!!

        val payPair = paymentManager.generatePayments(roomId, members.first, members.second)
        val firstHash = ByteArrayWrapper(payPair.first.rHash)
        val secondHash = ByteArrayWrapper(payPair.second.rHash)
        val msg1 = WsMsg(MsgType.HASH_DATA, arbiter.userInfo.id, HashData(roomId, firstHash, cost))
        val msg2 = WsMsg(MsgType.HASH_DATA, arbiter.userInfo.id, HashData(roomId, secondHash, cost))
        roomManager.rHash(roomId, members.first, firstHash)
        roomManager.rHash(roomId, members.first, secondHash)
        GlobalScope.launch {
            us1.send(doSign(msg1, arbiter.privateKey))
            us2.send(doSign(msg2, arbiter.privateKey))
        }
    }

    private fun queryUserCurrentRoom(sessionId: String): String? {
        val userId = changeSessionId2UserId(sessionId)!!
        return commonUserManager.queryCurrentRoom(userId)
    }

    override fun doSign(msg: WsMsg, priKey: PrivateKey): Frame.Text {
        return Frame.Text(SignService.doSign(msg, priKey).toJson())
    }

    private fun surrender(surrenderUserId: String, roomId: String, arbiter: UserSelf, leaveFlag: Boolean, tieFlag: Boolean = false) {
        println("do Surrender")
        val room = roomManager.queryRoomNotNull(roomId)
        var winnerUserId: String? = null
        if (leaveFlag && !room.isFull) {
            //do nothing
        } else {
            val playerUserId = room.rivalPlayer(surrenderUserId)!!
            val session = sessionManager.querySocketByUserId(playerUserId)!!

            if (!tieFlag) {
                winnerUserId = playerUserId
            }
            val r: ByteArray? = if (room.payment) paymentManager.surrenderR(surrenderUserId, roomId) else null

            GlobalScope.launch {
                session.send(doSign(WsMsg(MsgType.SURRENDER_RESP, arbiter.userInfo.id, SurrenderResp(roomId, r?.let { ByteArrayWrapper(r) })), arbiter.privateKey))
            }
        }
        doGameEnd(room, arbiter, winnerUserId)
    }

    private fun doGameEnd(room: Room, arbiter: UserSelf, winnerUserId: String? = null) {
        val gameEnd = GameEnd(room.roomId, winnerUserId?.let { winnerUserId })
        room.players.forEach { player ->
            val session = sessionManager.querySocketByUserId(player.playerUserId)!!
            GlobalScope.launch {
                session.send(doSign(WsMsg(MsgType.GAME_END, arbiter.userInfo.id, gameEnd), arbiter.privateKey))
            }
            commonUserManager.clearRoom(player.playerUserId)
        }

        roomManager.clearRoom(room.roomId)
        paymentManager.clearPaymentInfoByRoomId(room.roomId)
        println("do GameEnd, clear room")
    }

    private fun doException(err: String) {
        LOG.error(err)
        throw RuntimeException(err)
    }
}
