package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.starcoin.thor.core.LnConfig

lateinit var aliceMsgClient: MsgClientServiceImpl
lateinit var bobMsgClient: MsgClientServiceImpl

fun main(args: Array<String>) {
    createAlice()
    createBob()
    test2()
    runBlocking {
        delay(50000)
    }
}

fun test1() {
    val gameName = aliceMsgClient.createGame()
    val room = aliceMsgClient.createRoom(gameName)
    aliceMsgClient.joinFreeRoom(room.roomId!!)

    runBlocking {
        delay(1000)
    }
    bobMsgClient.joinFreeRoom(room.roomId!!)

    runBlocking {
        delay(1000)
    }

    aliceMsgClient.roomMsg(room.roomId!!, "test1 msg")
}

fun test2() {
    aliceMsgClient.createGame()
    val gameListResp = aliceMsgClient.queryGameList()
    gameListResp?.let {
        aliceMsgClient.doCreateRoom(gameListResp.data!![0].gameHash, 0)

        val roomId = aliceMsgClient.channelMsg()

        roomId?.let {
            bobMsgClient.joinFreeRoom(roomId)

            runBlocking {
                delay(1000)
            }
            aliceMsgClient.roomMsg(roomId, "test2 msg")
        }
    }

    val resp = aliceMsgClient.queryRoomList(gameListResp!!.data!![0].gameHash)
    println(resp!!.data2Str())
}

fun createAlice() {
    val aliceCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("alice.cert")
    val alicConfig = LnConfig(aliceCert, "starcoin-firstbox", 30009)
    aliceMsgClient = MsgClientServiceImpl(alicConfig)
    aliceMsgClient.start()
}

fun createBob() {
    val bobCert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream("bob.cert")
    val bobConfig = LnConfig(bobCert, "starcoin-firstbox", 40009)
    bobMsgClient = MsgClientServiceImpl(bobConfig)
    bobMsgClient.start()
}

