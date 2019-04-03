package org.starcoin.thor.server

interface RpcServer<S> {

    fun start()

    fun stop()

    fun registerService(service: S)

    @Throws(InterruptedException::class)
    fun awaitTermination()
}