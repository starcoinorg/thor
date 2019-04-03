package org.starcoin.lightning.client;

import io.grpc.Channel;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.proto.LightningGrpc;

public class SyncClient {

  private Channel channel;

  public SyncClient(Channel channel){
    this.channel = channel;
  }

  public void addInvoice(Invoice invoice){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
  }
}
