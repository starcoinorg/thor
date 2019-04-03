package org.starcoin.thor.server

import io.grpc.BindableService
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.Server

class GrpcServer : RpcServer<BindableService> {

    lateinit var server:Server

    override fun start() {
        server = NettyServerBuilder.forPort(8081).addService(GameServiceImpl()).build().start()
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun registerService(service: BindableService) {

    }

    override fun awaitTermination() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}