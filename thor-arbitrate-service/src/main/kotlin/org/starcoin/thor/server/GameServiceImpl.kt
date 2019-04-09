package org.starcoin.thor.server

import com.google.common.base.Preconditions
import com.google.protobuf.Empty
import io.grpc.stub.StreamObserver
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.proto.Thor
import org.starcoin.thor.proto.GameServiceGrpc

class GameServiceImpl(private val gameManager: GameManager) : GameServiceGrpc.GameServiceImplBase() {

    private val size = 10

    override fun createGame(request: Thor.CreateGameReq, responseObserver: StreamObserver<Thor.SuccResp>) {
        Preconditions.checkNotNull(request.game.addr)
        Preconditions.checkNotNull(request.game.name)
        Preconditions.checkNotNull(request.game.gameHash)
        Preconditions.checkArgument(request.game.cost > 0)

        val tmpGame = GameInfo.paseFromProto(request.game)
        var flag = gameManager.createGame(tmpGame)

        responseObserver.onNext(Thor.SuccResp.newBuilder().setSucc(flag).build())
        responseObserver.onCompleted()
    }

    override fun gameList(request: Thor.GameListReq, responseObserver: StreamObserver<Thor.GameListResp>) {
        val begin = when (request.page <= 0) {
            true -> 0
            false -> (request.page - 1) * size
        }

        val count = gameManager.count()
        val end = when (begin + size < count) {
            true -> begin + size
            false -> count
        }

        val data = gameManager.list(begin, end)

        responseObserver.onNext(Thor.GameListResp.newBuilder().setTotal(count).addAllList(data).build())
        responseObserver.onCompleted()
    }

    override fun queryGame(request: Thor.QueryGameReq, responseObserver: StreamObserver<Thor.QueryGameResp>) {
        val game = gameManager.queryGameByHash(request.gameHash)
        val builder = Thor.QueryGameResp.newBuilder().setHasGame(false)
        game?.let {
            builder.setHasGame(true).setGame(game.toGameProto())
        }
        responseObserver.onNext(builder.build())
        responseObserver.onCompleted()
    }

    override fun queryAdmin(request: Empty?, responseObserver: StreamObserver<Thor.ProtoAdmin>) {
        responseObserver.onNext(Thor.ProtoAdmin.newBuilder().setAddr(gameManager.adminAddr).build())
        responseObserver.onCompleted()
    }
}