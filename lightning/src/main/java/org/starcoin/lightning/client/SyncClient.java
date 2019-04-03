package org.starcoin.lightning.client;

import io.grpc.Channel;

import com.google.protobuf.ByteString;

import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.proto.LightningGrpc;
import org.starcoin.lightning.proto.LightningOuterClass;

public class SyncClient {

  private Channel channel;

  public SyncClient(Channel channel){
    this.channel = channel;
  }

  public void addInvoice(Invoice invoice){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.Invoice.Builder invoiceBuilder = LightningOuterClass.Invoice.newBuilder();
    invoiceBuilder.setRHash(ByteString.copyFrom(invoice.getrHash()));
    invoiceBuilder.setValue(invoice.getValue());
    LightningOuterClass.AddInvoiceResponse response = stub.addInvoice(invoiceBuilder.build());
    String payMentRequest = response.getPaymentRequest();
    
  }
}
