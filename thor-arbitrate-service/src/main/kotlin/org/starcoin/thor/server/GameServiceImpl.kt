package org.starcoin.thor.server

import com.google.common.base.Preconditions
import io.grpc.stub.StreamObserver
import org.starcoin.thor.core.GameInfo
import org.starcoin.thor.proto.Thor
import org.starcoin.thor.proto.GameServiceGrpc
import org.starcoin.sirius.lang.toHEXString
import java.util.*

class GameServiceImpl : GameServiceGrpc.GameServiceImplBase() {

    private val appMap = mutableMapOf<String, GameInfo>()
    private val nameSet = mutableSetOf<String>()
    private val gameHashSet = mutableSetOf<String>()
    private val count:Int = 0
    private val size = 10

    override fun createGame(request: Thor.CreateGameReq?, responseObserver: StreamObserver<Thor.SuccResp>?) {
        Preconditions.checkNotNull(request!!.game.addr)
        Preconditions.checkNotNull(request!!.game.name)
        Preconditions.checkNotNull(request!!.game.gameHash)
        Preconditions.checkArgument(request!!.game.cost > 0)

        val tmpGame = GameInfo.paseFromProto(request.game)
        var flag = false
        synchronized(this) {
            when (!nameSet.contains(tmpGame.name) && !gameHashSet.contains(tmpGame.gameHash.toHEXString())) {
                true -> {
                    nameSet.add(tmpGame.name)
                    gameHashSet.add(tmpGame.gameHash.toHEXString())
                    appMap[tmpGame.gameHash.toHEXString()] = tmpGame
                    count.inc()
                    flag = true
                }
            }
        }

        responseObserver!!.onNext(Thor.SuccResp.newBuilder().setSucc(flag).build())
    }

    override fun gameStart(request: Thor.GameStartReq?, responseObserver: StreamObserver<Thor.GameStartResp>?) {

    }

    override fun gameList(request: Thor.GameListReq?, responseObserver: StreamObserver<Thor.GameListResp>?) {
        val begin = when(request!!.page <= 0) {
            true -> 0
            false -> (request!!.page - 1) * size
        }

        val end = when(begin + size < count) {
            true -> begin + size
            false -> count
        }

        var keys = gameHashSet.toList().subList(begin, end).toSet()
        val data = appMap.filterKeys { keys.contains(it) }.values.map{ it.toProto<Thor.ProtoGameInfo>() }

        responseObserver!!.onNext(Thor.GameListResp.newBuilder().setTotal(count).addAllList(data).build())
    }

    override fun joinGame(request: Thor.JoinGameReq?, responseObserver: StreamObserver<Thor.SuccResp>?) {

    }
}