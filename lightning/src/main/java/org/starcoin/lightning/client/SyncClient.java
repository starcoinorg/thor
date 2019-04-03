package org.starcoin.lightning.client;

import io.grpc.Channel;

import java.util.logging.Logger;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.Payment;
import org.starcoin.lightning.client.core.PaymentResponse;
import org.starcoin.lightning.proto.LightningGrpc;
import org.starcoin.lightning.proto.LightningOuterClass;

public class SyncClient {

  private Logger logger = Logger.getLogger(SyncClient.class.getName());
  private Channel channel;

  public SyncClient(Channel channel){
    this.channel = channel;
  }

  public AddInvoiceResponse addInvoice(Invoice invoice){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.AddInvoiceResponse response = stub.addInvoice(invoice.toProto());
    return AddInvoiceResponse.copyFrom(response);
  }

  public PaymentResponse sendPayment(Payment payment){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.SendResponse response= stub.sendPaymentSync(payment.toProto());
    logger.info(response.toString());
    return PaymentResponse.copyFrom(response);
  }

}
