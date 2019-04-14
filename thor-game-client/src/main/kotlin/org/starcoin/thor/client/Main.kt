package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.starcoin.thor.core.LnConfig

fun main(args: Array<String>) {
    val aliceCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("alice.cert")
    val alicConfig = LnConfig(aliceCert, "starcoin-firstbox", 30009)
    val aliceMsgClient = MsgClientServiceImpl(alicConfig)
    aliceMsgClient.start()
    val gameName = aliceMsgClient.createGame()
    val room = aliceMsgClient.createRoom(gameName)
    aliceMsgClient.joinRoom(room.room!!)

    val bobCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("bob.cert")
    val bobConfig = LnConfig(bobCert, "starcoin-firstbox", 40009)
    val bobMsgClient = MsgClientServiceImpl(bobConfig)
    bobMsgClient.start()
    runBlocking {
        delay(1000)
    }
    bobMsgClient.joinRoom(room.room!!)

    runBlocking {
        delay(1000)
    }

    aliceMsgClient.roomMsg(room.room!!, "test msg")

    runBlocking {
        delay(50000)
    }
}

