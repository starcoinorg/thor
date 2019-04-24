package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.*
import org.starcoin.thor.sign.SignService

lateinit var aliceMsgClient: MsgClientServiceImpl
lateinit var bobMsgClient: MsgClientServiceImpl

fun main(args: Array<String>) {
    aliceMsgClient = newClientUser("alice.cert", "starcoin-firstbox", 30009)
    bobMsgClient = newClientUser("bob.cert", "starcoin-firstbox", 40009)
    val flag = java.util.Random().nextBoolean()
    println("---->$flag")
    test2(false)
    runBlocking {
        delay(50000)
    }
}

fun test1(flag: Boolean) {
    val resp = aliceMsgClient.createGame()
    runBlocking {
        delay(1000)
    }
    val room = aliceMsgClient.createRoom(resp.game!!.hash)
    runBlocking {
        delay(1000)
    }
    aliceMsgClient.joinRoom(room.room!!.roomId)

    runBlocking {
        delay(1000)
    }
    bobMsgClient.joinRoom(room.room!!.roomId)

    runBlocking {
        delay(1000)
    }

    aliceMsgClient.doReady(room.room!!.roomId, true)
    bobMsgClient.doReady(room.room!!.roomId, true)

    aliceMsgClient.roomMsg(room.room!!.roomId, "test1 msg")
}

fun test2(flag: Boolean) {
    aliceMsgClient.createGame()
    runBlocking {
        delay(1000)
    }
    val gameListResp = aliceMsgClient.queryGameList()
    runBlocking {
        delay(1000)
    }
    val datas = mutableListOf<WitnessData>()
    gameListResp?.let {
        aliceMsgClient.doCreateRoom(gameListResp.data!![0].hash, 1)

        runBlocking {
            delay(1000)
        }

        val aliceJson = aliceMsgClient.channelMsg()
        val aliceRoom = MsgObject.fromJson(aliceJson, Room::class)
        val aliceNum = aliceRoom.players.size

        bobMsgClient.joinRoom(aliceRoom.roomId)

        val bobJson = bobMsgClient.channelMsg()
        val bobRoom = MsgObject.fromJson(bobJson, Room::class)
        val bobNum = bobRoom.players.size

        println("wait begin")
        runBlocking {
            delay(10000)
        }
        println("wait end")
        aliceMsgClient.doReady(aliceRoom.roomId, false)
        bobMsgClient.doReady(bobRoom.roomId, false)

        runBlocking {
            delay(1000)
        }

        aliceMsgClient.roomMsg(aliceRoom.roomId, "test2 msg")

        runBlocking {
            delay(1000)
        }

        val wd = WitnessData("stateHash", SignService.sign(longToBytes(System.currentTimeMillis()), bobMsgClient.priKey()), ByteArrayWrapper("test game msg".toByteArray()))
        bobMsgClient.doRoomGameDataReq(bobRoom.roomId, wd)
        datas.add(wd)
        runBlocking {
            delay(10000)
        }
    }

    val resp = aliceMsgClient.queryRoomList(gameListResp!!.data!![0].hash)
    println(resp!!.toJson())

    if (flag)
        aliceMsgClient.doSurrenderReq()
    else
        bobMsgClient.doChallenge(datas)
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

