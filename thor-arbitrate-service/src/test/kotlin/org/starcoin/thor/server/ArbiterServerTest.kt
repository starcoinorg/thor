package org.starcoin.thor.server

import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.lang.toNoPrefixHEXString
import org.starcoin.sirius.util.MockUtils
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.core.LnClient
import org.starcoin.thor.core.LnConfig

class ArbiterServerTest {

    private lateinit var player1:User
    private lateinit var player2:User

    @Before
    fun initTest() {
        //start server
        val puncherCert = WebsocketServer::class.java.classLoader.getResourceAsStream("arb.cert")
        val puncherConfig = LnConfig(puncherCert, "starcoin-firstbox", 20009)
        val puncherClient = LnClient(puncherConfig)
        puncherClient.start()
        puncherConfig.addr = puncherClient.syncClient.identityPubkey
        val gameManager = GameManager(puncherConfig.addr!!)
        val msgServer = WebsocketServer(gameManager, puncherClient)
        msgServer.start()

        //regist game
        val hash = MockUtils.nextBytes(20).toNoPrefixHEXString()
        val game = GameInfo(hash, hash, hash, 1)

        //game player
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
}