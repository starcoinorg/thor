package org.starcoin.thor.core

import io.grpc.Channel
import org.starcoin.lightning.client.SyncClient
import org.starcoin.lightning.client.Utils
import java.io.InputStream


data class LnConfig(val addr: String, val cert: InputStream, val host: String, val port: Int)

class LnClient(val conf: LnConfig) {
    private lateinit var chan: Channel
    lateinit var syncClient: SyncClient

    fun start() {
        chan = Utils.buildChannel(conf.cert, conf.host, conf.port)
        syncClient = SyncClient(chan)
    }

}