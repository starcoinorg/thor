package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.starcoin.thor.core.*

class MsgServiceImpl {

    private val paymentManager = PaymentManager()
    private val gameManager = GameManager()
    private val userManager = UserManager()
    private val roomManager = RoomManager()
    private val sessionConfirmManager = SessionConfirmManager()
    private val size = 10

    fun doConnection(newUser: User) {
        newUser.stat = UserStatus.CONFIRMED
        userManager.addUser(newUser)
        //TODO("return CONFIRM_REQ and use addUser()")
    }

    fun doConfirm(sessionId: String): SessionConfirm {
        return sessionConfirmManager.generateSessionConfirm(sessionId)
    }

    fun queryConfirmInfo(sessionId: String): ConfirmInfo? {
        return userManager.queryConfirmInfo(sessionId)
    }

    fun doConfirmInfo(sessionId: String, paymentRequest: String) {
        userManager.confirmInfo(sessionId, paymentRequest)
    }

    fun doPaymentSettled(sessionId: String) {
        userManager.paymentSettled(sessionId)
    }

    fun doGameBegin(members: Pair<String, String>, roomId: String) {
        val u1 = userManager.queryUser(members.first)!!
        val u2 = userManager.queryUser(members.second)!!
        val room = roomManager.getRoom(roomId)
        val begin = BeginMsg2(room).data2Str()
        val msg1 = WsMsg(MsgType.GAME_BEGIN, begin, from = null, to = u1.sessionId)
        val msg2 = WsMsg(MsgType.GAME_BEGIN, begin, from = null, to = u2.sessionId)
        val us = Pair(u1.sessionId, u2.sessionId)
        val flag = userManager.gameBegin(us)
        if (flag) {
            GlobalScope.launch {
                u1.session.send((Frame.Text(msg1.msg2Str())))
                u2.session.send((Frame.Text(msg2.msg2Str())))
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
                        GlobalScope.launch { toU.session.send(Frame.Text(WsMsg(MsgType.SURRENDER_RESP, resp, surrenderAddr, toU.sessionId!!).msg2Str())) }
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

    fun doBroadcastRoomMsg(fromUser: String, users: List<String>?, msg: String) {
        users?.let { users.forEach { doOther(WsMsg(MsgType.ROOM_DATA_MSG, msg, fromUser, it)) } }
    }

    fun doRoomPaymentMsg(users: List<String>?, msg: WsMsg) {
        users?.let { users.forEach { doOther(msg) } }
    }

    fun doJoinRoom(sessionId: String, room: String): Room {
        return roomManager.joinRoom(sessionId, room)
    }

    fun doPayment(sessionId: String, room: String): Room {
        return roomManager.paymentRoom(sessionId, room)
    }

    fun doPayments(members: Pair<String, String>, roomId: String, cost: Long) {
        val u1 = userManager.queryUser(members.first)!!
        val u2 = userManager.queryUser(members.second)!!
        val payPair = paymentManager.generatePayments(roomId, members.first, members.second)
        val msg1 = WsMsg(MsgType.GAME_BEGIN, PaymentReq(roomId, payPair.first.rHash, cost).data2Str(), from = null, to = u1.sessionId)
        val msg2 = WsMsg(MsgType.GAME_BEGIN, PaymentReq(roomId, payPair.second.rHash, cost).data2Str(), from = null, to = u2.sessionId)
        GlobalScope.launch {
            u1.session.send((Frame.Text(msg1.msg2Str())))
            u2.session.send((Frame.Text(msg2.msg2Str())))
        }
    }

    fun doCreateRoom(game: String, deposit: Long): Room {
        Preconditions.checkArgument(deposit >= 0)
        val gameInfo = gameManager.queryGameByHash(game) ?: throw NotFoundException("Can not find game by hash: $game")
        return roomManager.createRoom(gameInfo, deposit)
    }

    fun getRoomOrNull(roomId: String): Room? {
        return roomManager.getRoomOrNull(roomId)
    }

    fun getRoom(roomId: String): Room {
        return roomManager.getRoom(roomId)
    }

    fun doRoomList(game: String): List<Room> {
        return roomManager.queryRoomListByGame(game)
    }

    fun doRoomList(): List<Room> {
        return roomManager.queryAllRoomList()
    }

    fun doCreateGame(gameInfo: GameInfo) {
        gameManager.createGame(gameInfo)
    }

    fun doGameList(page: Int): GameListResp {
        val begin = when (page <= 0) {
            true -> 0
            false -> (page - 1) * size
        }

        val count = gameManager.count()
        val end = when (begin + size < count) {
            true -> begin + size
            false -> count
        }

        val data = gameManager.list(begin, end)
        return GameListResp(count, data)
    }
}