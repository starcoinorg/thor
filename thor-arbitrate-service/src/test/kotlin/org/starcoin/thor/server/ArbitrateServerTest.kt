package org.starcoin.thor.server

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.toHexString
import org.junit.Before
import org.junit.Test
import org.starcoin.thor.core.MsgObject
import org.starcoin.thor.core.Room
import org.starcoin.thor.core.UserInfo
import org.starcoin.thor.core.UserSelf
import org.starcoin.thor.sign.SignService
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths

class ArbitrateServerTest {

    lateinit var aliceMsgClient: MsgClientServiceImpl
    lateinit var bobMsgClient: MsgClientServiceImpl

    @Before
    fun initTest() {
        aliceMsgClient = newClientUser("/tmp/thor/lnd/lnd_alice/tls.cert", "localhost", 30009, "/tmp/thor/lnd/lnd_alice/data/chain/bitcoin/simnet/admin.macaroon")
        bobMsgClient = newClientUser("/tmp/thor/lnd/lnd_bob/tls.cert", "localhost", 40009, "/tmp/thor/lnd/lnd_bob/data/chain/bitcoin/simnet/admin.macaroon")
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
    fun test1() {
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
}