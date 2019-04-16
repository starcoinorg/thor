package org.starcoin.thor.server

import io.grpc.Channel
import org.junit.Before
import org.junit.Test
import org.starcoin.lightning.client.SyncClient
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.core.LnConfig
import sun.net.www.http.HttpClient
import java.util.*

class ArbiterServerTest {

    private lateinit var msgServer: WebsocketServer
    private lateinit var player1: User
    private lateinit var player2: User

    @Before
    fun initTest() {
        val arbiterCert = WebsocketServer::class.java.classLoader.getResourceAsStream("arb.cert")
        val arbiterConfig = LnConfig(arbiterCert, "starcoin-firstbox", 20009)

        msgServer = WebsocketServer(arbiterConfig)
        msgServer.start()

        val testGame = "test-game-" + Random().nextLong()
        val gameInfo = GameInfo(testGame, testGame, testGame)
        msgServer.msgService.doCreateGame(gameInfo)

        val aliceCert = this.javaClass.classLoader.getResourceAsStream("alice.cert")
        val alicConfig = LnConfig(aliceCert, "starcoin-firstbox", 30009)

        val bobCert = this.javaClass.classLoader.getResourceAsStream("bob.cert")
        val bobConfig = LnConfig(bobCert, "starcoin-firstbox", 40009)
    }

    @Test
    fun testUnpay() {

    }

    @Test
    fun testPay() {

    }

    @Test
    fun testSurrender() {

    }

    @Test
    fun testChallengeSucc() {

    }

    @Test
    fun testChallengeFail() {

    }

    class test {
        private lateinit var chan: Channel
        private lateinit var syncClient: SyncClient

        private lateinit var client: HttpClient
    }
}