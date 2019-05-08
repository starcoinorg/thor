package org.starcoin.thor.server

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.toHexString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.starcoin.sirius.serialization.ByteArrayWrapper
import org.starcoin.thor.core.*
import org.starcoin.thor.manager.GameManager
import org.starcoin.thor.manager.RoomManager
import org.starcoin.thor.sign.SignService
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

class ArbitrateServerTest {

    lateinit var aliceMsgClient: MsgClientServiceImpl
    lateinit var bobMsgClient: MsgClientServiceImpl
    lateinit var websocketServer: WebsocketServer

    @Before
    fun before() {
        val gameManager = GameManager()
        val roomManager = RoomManager()
        loadGames().forEach { game ->
            gameManager.createGame(game)
        }
        websocketServer = WebsocketServer(gameManager, roomManager)
        websocketServer.start(false)
        aliceMsgClient = newClientUser("/tmp/thor/lnd/lnd_alice/tls.cert", "localhost", 10009, "/tmp/thor/lnd/lnd_alice/data/chain/bitcoin/simnet/admin.macaroon")
        bobMsgClient = newClientUser("/tmp/thor/lnd/lnd_bob/tls.cert", "localhost", 20009, "/tmp/thor/lnd/lnd_bob/data/chain/bitcoin/simnet/admin.macaroon")
    }

    private fun newClientUser(fileName: String, host: String, port: Int, macarron: String): MsgClientServiceImpl {
        val keyPair = SignService.generateKeyPair()
        val userInfo = UserInfo(keyPair.public)
        val cert = FileInputStream(fileName)
        val path = Paths.get(macarron)
        val data = Files.readAllBytes(path).toHexString()
        val config = LnConfig(cert, host, port, data)
        val clientUser = ClientUser(UserSelf(keyPair.private, userInfo), config)
        val msgClientService = MsgClientServiceImpl(clientUser)
        msgClientService.start()

        return msgClientService
    }

    @Test
    fun testSendMsg() {
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

    @Test
    fun testGame() {
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

            aliceMsgClient.joinRoom(aliceRoom.roomId)
            aliceMsgClient.channelMsg()
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

                val challengeFlag = java.util.Random().nextBoolean()
                if (challengeFlag) {
                    aliceMsgClient.doSurrenderReq(aliceRoom.roomId)
                    runBlocking {
                        delay(1000)
                    }
                    println("------")
                    assert(bobMsgClient.hasR())
                } else
                    bobMsgClient.doChallenge(datas)
            }
        }

        println("test end")
    }

    @After
    fun after() {
        aliceMsgClient.stop()
        bobMsgClient.stop()
        websocketServer.stop()
    }
}