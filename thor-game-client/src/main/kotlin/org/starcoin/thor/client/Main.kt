package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.starcoin.sirius.lang.toNoPrefixHEXString
import org.starcoin.sirius.util.MockUtils
import org.starcoin.thor.core.LnClient
import org.starcoin.thor.core.LnConfig

fun main(args: Array<String>) {
    val hash = MockUtils.nextBytes(20).toNoPrefixHEXString()
    val gameClient = GameClientServiceImpl()
    gameClient.start()
    gameClient.registGame(hash)
    val game = gameClient.queryGame(hash)

    val aliceCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("alice.cert")
    val alicConfig = LnConfig(aliceCert, "starcoin-firstbox", 30009)
    val aliceClient = LnClient(alicConfig)
    aliceClient.start()
    alicConfig.addr = aliceClient.syncClient.identityPubkey
    val aliceMsgClient = MsgClientServiceImpl(aliceClient)
    aliceMsgClient.start()

    val bobCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("bob.cert")
    val bobConfig = LnConfig(bobCert, "starcoin-firstbox", 40009)
    val bobClient = LnClient(bobConfig)
    bobClient.start()
    bobConfig.addr = bobClient.syncClient.identityPubkey
    val bobMsgClient = MsgClientServiceImpl(bobClient)
    bobMsgClient.start()

    runBlocking {
        delay(10000)
    }

    aliceMsgClient.doStartAndInviteReq(game.gameHash, bobConfig.addr!!)

    runBlocking {
        delay(10000)
    }

    val flag = java.util.Random().nextBoolean()
    if (flag) {
        println("Surrender")
        bobMsgClient.doSurrenderReq()
    } else {
        println("Challenge")
        bobMsgClient.doChallenge()
    }

    runBlocking {
        delay(20000)
    }

    println("test end")
}

