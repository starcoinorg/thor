package org.starcoin.thor.server

import io.grpc.Channel
import org.starcoin.lightning.client.SyncClient

class Account(channel: Channel?, val amount: Long) : SyncClient(channel) {
    val preimage: Preimage = Preimage()
}