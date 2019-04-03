package org.starcoin.thor.server

import io.grpc.stub.StreamObserver
import org.starcoin.thor.proto.Thor
import org.starcoin.thor.proto.GameServiceGrpc

class GameServiceImpl : GameServiceGrpc.GameServiceImplBase() {

    override fun createGame(request: Thor.CreateGameReq?, responseObserver: StreamObserver<Thor.SuccResp>?) {
        super.createGame(request, responseObserver)
    }

    override fun gameStart(request: Thor.GameStartReq?, responseObserver: StreamObserver<Thor.GameStartResp>?) {
        super.gameStart(request, responseObserver)
    }

    override fun gameList(request: Thor.GameListReq?, responseObserver: StreamObserver<Thor.GameListResp>?) {
        super.gameList(request, responseObserver)
    }

    override fun joinGame(request: Thor.JoinGameReq?, responseObserver: StreamObserver<Thor.SuccResp>?) {
        super.joinGame(request, responseObserver)
    }
}