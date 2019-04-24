package org.starcoin.thor.server

import io.ktor.http.cio.websocket.DefaultWebSocketSession
import io.ktor.http.cio.websocket.Frame
import org.starcoin.thor.core.Room
import org.starcoin.thor.core.UserInfo
import org.starcoin.thor.core.UserSelf
import org.starcoin.thor.core.WsMsg
import java.security.PrivateKey
import java.security.PublicKey

interface PlayService {

    fun sendNonce(sessionId: String, session: DefaultWebSocketSession): String

    fun clearSession(sessionId: String)

    fun storePubKey(sessionId: String, userInfo: UserInfo)

    fun compareNoce(sessionId: String, nonce: String): Boolean

    fun queryPubKey(sessionId: String): PublicKey?

    fun doCreateRoom(game: String, cost: Long, time: Long, sessionId: String?): Room?

    fun doJoinRoom(sessionId: String, roomId: String, arbiter: UserSelf)

    fun doSign(msg: WsMsg, priKey: PrivateKey): Frame.Text
}