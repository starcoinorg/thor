package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.ktor.features.NotFoundException
import io.ktor.http.cio.websocket.DefaultWebSocketSession
import org.starcoin.thor.core.JoinRoomReq
import org.starcoin.thor.core.Room
import org.starcoin.thor.core.UserInfo
import org.starcoin.thor.manager.CommonUserManager
import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager
import org.starcoin.thor.manager.SessionManager
import java.security.PublicKey

class PlayServiceImpl(private val gameManager: GameManager) : PlayService {

    private val sessionManager = SessionManager()
    private val commonUserManager = CommonUserManager()
    private val roomManager = RoomManager()

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

    override fun changeSessionId2UserId(sessionId: String): String? {
        return sessionManager.queryUserId(sessionId)
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

    override fun queryUserCurrentRoom(sessionId: String): String? {
        val userId = changeSessionId2UserId(sessionId)
        return userId?.let { commonUserManager.queryCurrentRoom(userId) }
    }

    override fun doJoinRoom(userId: String, roomId: String): Room {
        return roomManager.joinRoom(userId, roomId)
    }
}