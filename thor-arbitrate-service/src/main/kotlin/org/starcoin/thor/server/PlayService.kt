package org.starcoin.thor.server

import io.ktor.http.cio.websocket.DefaultWebSocketSession
import org.starcoin.thor.core.Room
import org.starcoin.thor.core.UserInfo
import java.security.PublicKey

interface PlayService {

    fun sendNonce(sessionId: String, session: DefaultWebSocketSession): Long

    fun clearSession(sessionId: String)

    fun storePubKey(sessionId: String, userInfo: UserInfo)

    fun compareNoce(sessionId: String, nonce: Long): Boolean

    fun queryPubKey(sessionId: String): PublicKey?

    fun doCreateRoom(game: String, deposit: Long, time: Long, sessionId: String?): Room?

    fun changeSessionId2UserId(sessionId: String): String?

    fun queryUserCurrentRoom(sessionId: String): String?

    fun doJoinRoom(userId: String, roomId: String): Room
}