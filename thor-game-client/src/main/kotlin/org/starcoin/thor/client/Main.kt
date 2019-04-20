package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.starcoin.thor.core.UserInfo
import org.starcoin.thor.core.UserSelf
import org.starcoin.thor.sign.SignService

lateinit var aliceMsgClient: MsgClientServiceImpl
lateinit var bobMsgClient: MsgClientServiceImpl

fun main(args: Array<String>) {
    aliceMsgClient = newClientUser("alice.cert", "starcoin-firstbox", 30009)
    bobMsgClient = newClientUser("bob.cert", "starcoin-firstbox", 40009)
    test2()
    runBlocking {
        delay(50000)
    }
}

fun test1() {
    val gameName = aliceMsgClient.createGame()
    runBlocking {
        delay(1000)
    }
    val room = aliceMsgClient.createRoom(gameName)
    runBlocking {
        delay(1000)
    }
    aliceMsgClient.joinRoom(room.roomId!!)

    runBlocking {
        delay(1000)
    }
    bobMsgClient.joinRoom(room.roomId!!)

    runBlocking {
        delay(1000)
    }

    aliceMsgClient.roomMsg(room.roomId!!, "test1 msg")
}

fun test2() {
    aliceMsgClient.createGame()
    runBlocking {
        delay(1000)
    }
    val gameListResp = aliceMsgClient.queryGameList()
    runBlocking {
        delay(1000)
    }
    gameListResp?.let {
        aliceMsgClient.doCreateRoom(gameListResp.data!![0].hash, 1)

        runBlocking {
            delay(1000)
        }

        val roomId = aliceMsgClient.channelMsg()
        println("--->$roomId")

        roomId?.let {
            bobMsgClient.joinRoom(roomId)

            println("wait begin")
            runBlocking {
                delay(10000)
            }
            println("wait end")
            aliceMsgClient.checkInvoiceAndReady(roomId)
            bobMsgClient.checkInvoiceAndReady(roomId)

            aliceMsgClient.roomMsg(roomId, "test2 msg")
        }
    }

    val resp = aliceMsgClient.queryRoomList(gameListResp!!.data!![0].hash)
    println(resp!!.data2Str())

    aliceMsgClient.doSurrenderReq()
}

fun newClientUser(fileName: String, host: String, port: Int): MsgClientServiceImpl {
    val keyPair = SignService.generateKeyPair()
    val userInfo = UserInfo(keyPair.public)
    val cert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream(fileName)
    val config = LnConfig(cert, host, port)
    val clientUser = ClientUser(UserSelf(keyPair.private, userInfo), config)
    val msgClientService = MsgClientServiceImpl(clientUser)
    msgClientService.start()

    return msgClientService
}

