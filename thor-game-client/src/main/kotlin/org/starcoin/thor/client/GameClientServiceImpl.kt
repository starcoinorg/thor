package org.starcoin.thor.client

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.starcoin.sirius.core.InetAddressPort
import org.starcoin.sirius.lang.toHEXString
import org.starcoin.sirius.serialization.toByteString
import org.starcoin.sirius.util.MockUtils
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.proto.GameServiceGrpc
import org.starcoin.thor.proto.Thor

class GameClientServiceImpl {

    private lateinit var gameChannel: ManagedChannel
    private lateinit var gameStub: GameServiceGrpc.GameServiceBlockingStub
    fun start(addr: String = "127.0.0.1:8081") {
        gameChannel = NettyChannelBuilder.forAddress(InetAddressPort.valueOf(addr).toInetSocketAddress()).usePlaintext().build()
        gameStub = GameServiceGrpc.newBlockingStub(gameChannel)
    }

    fun registGame(hash: ByteString) {
        val game1 = Thor.ProtoGameInfo.newBuilder().setCost(10).setName("testgame1").setGameHash(hash).setAddr(MockUtils.nextBytes(20).toByteString()).build()
        val createGame = Thor.CreateGameReq.newBuilder().setGame(game1).build()
        gameStub.createGame(createGame)
    }

    fun gameList() {
        val req = Thor.GameListReq.newBuilder().setPage(1).build()
        val resp = gameStub.gameList(req)
    }

    fun queryGame(gameHash: ByteArray): GameInfo {
        val req = Thor.QueryGameReq.newBuilder().setGameHash(gameHash.toByteString()).build()
        val resp = gameStub.queryGame(req)
        return GameInfo.paseFromProto(resp.game)
    }

    fun queryAdmin(): String {
        var resp = gameStub.queryAdmin(com.google.protobuf.Empty.newBuilder().build())
        return resp.addr!!.toByteArray().toHEXString()
    }
}