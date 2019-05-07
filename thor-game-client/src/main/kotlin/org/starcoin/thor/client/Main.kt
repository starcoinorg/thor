package org.starcoin.thor.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.*
import org.starcoin.thor.sign.SignService

lateinit var aliceMsgClient: MsgClientServiceImpl
lateinit var bobMsgClient: MsgClientServiceImpl

fun main(args: Array<String>) {
    aliceMsgClient = newClientUser("alice.cert", "starcoin-firstbox", 30009, "0201036c6e6402cf01030a103f34bc89eba51787c5d7cc6f9fab8afa1201301a160a0761646472657373120472656164120577726974651a130a04696e666f120472656164120577726974651a170a08696e766f69636573120472656164120577726974651a160a076d657373616765120472656164120577726974651a170a086f6666636861696e120472656164120577726974651a160a076f6e636861696e120472656164120577726974651a140a057065657273120472656164120577726974651a120a067369676e6572120867656e657261746500000620a0c4d52bd9351e0cfeba45fe9375fc5631e06dd2cf11eeb291900d2444d6bb8c")
    bobMsgClient = newClientUser("bob.cert", "starcoin-firstbox", 40009, "0201036c6e6402cf01030a1062636341698aadb76c3903354d113be71201301a160a0761646472657373120472656164120577726974651a130a04696e666f120472656164120577726974651a170a08696e766f69636573120472656164120577726974651a160a076d657373616765120472656164120577726974651a170a086f6666636861696e120472656164120577726974651a160a076f6e636861696e120472656164120577726974651a140a057065657273120472656164120577726974651a120a067369676e6572120867656e6572617465000006202836c12717f72e94b28b850486196f2d7b38299017fdb68d5ab11cb0bb2c7a58")
    val flag = java.util.Random().nextBoolean()
    test2(flag)
    runBlocking {
        delay(50000)
    }
}

fun test1(flag: Boolean) {
    val resp = aliceMsgClient.queryGameList()
    runBlocking {
        delay(1000)
    }
    aliceMsgClient.createRoom(resp!!.data!![0].hash, "test-1-room", 1)
    val room = MsgObject.fromJson(aliceMsgClient.channelMsg(), Room::class)
    runBlocking {
        delay(1000)
    }
    aliceMsgClient.joinRoom(room.roomId)

    runBlocking {
        delay(1000)
    }
    bobMsgClient.joinRoom(room.roomId)

    runBlocking {
        delay(1000)
    }

    aliceMsgClient.doReady(room.roomId, true)
    bobMsgClient.doReady(room.roomId, true)

    aliceMsgClient.roomMsg(room.roomId, "test1 msg")
}

fun test2(flag: Boolean) {
    val gameListResp = aliceMsgClient.queryGameList()
    runBlocking {
        delay(1000)
    }
    val datas = mutableListOf<WitnessData>()
    gameListResp?.let {
        aliceMsgClient.doCreateRoom(gameListResp.data!![0].hash, "test-2-room", 1)

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
            delay(10000)
        }

        aliceMsgClient.roomMsg(aliceRoom.roomId, "test2 msg")

        val resp = aliceMsgClient.queryRoomList(gameListResp.data!![0].hash)
        println(resp!!.toJson())

        runBlocking {
            delay(1000)
        }

        val leave = java.util.Random().nextBoolean()
        if (leave) {
            bobMsgClient.doLeaveRoom(bobRoom.roomId)
        } else {

            val wd = WitnessData(bobMsgClient.clientUser.self.userInfo.id, ByteArrayWrapper("stateHash".toByteArray()), SignService.sign(longToBytes(System.currentTimeMillis()), bobMsgClient.priKey()), ByteArrayWrapper("test game msg".toByteArray()))
            bobMsgClient.doRoomGameDataReq(bobRoom.roomId, wd)
            datas.add(wd)
            runBlocking {
                delay(10000)
            }

            if (flag)
                aliceMsgClient.doSurrenderReq(aliceRoom.roomId)
            else
                bobMsgClient.doChallenge(datas)
        }
    }

    println("test end")
}

fun newClientUser(fileName: String, host: String, port: Int, macarron: String): MsgClientServiceImpl {
    val keyPair = SignService.generateKeyPair()
    val userInfo = UserInfo(keyPair.public)
    val cert = MsgClientServiceImpl::class.java.classLoader.getResourceAsStream(fileName)
    val config = LnConfig(cert, host, port, macarron)
    val clientUser = ClientUser(UserSelf(keyPair.private, userInfo), config)
    val msgClientService = MsgClientServiceImpl(clientUser)
    msgClientService.start()

    return msgClientService
}
