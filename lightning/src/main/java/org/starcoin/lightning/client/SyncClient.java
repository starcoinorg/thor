package org.starcoin.lightning.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;

import java.util.List;
import java.util.logging.Logger;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.InvoiceList;
import org.starcoin.lightning.client.core.PayReq;
import org.starcoin.lightning.client.core.Payment;
import org.starcoin.lightning.client.core.PaymentResponse;
import org.starcoin.lightning.proto.LightningGrpc;
import org.starcoin.lightning.proto.LightningOuterClass;
import org.starcoin.lightning.proto.LightningOuterClass.ListInvoiceResponse;

public class SyncClient {

  private Logger logger = Logger.getLogger(SyncClient.class.getName());
  private Channel channel;

  public SyncClient(Channel channel){
    this.channel = channel;
  }

  public AddInvoiceResponse addInvoice(Invoice invoice){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.AddInvoiceResponse response = stub.addInvoice(invoice.toProto());
    logger.info(response.toString());
    return AddInvoiceResponse.copyFrom(response);
  }

  public PaymentResponse sendPayment(Payment payment){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.SendResponse response= stub.sendPaymentSync(payment.toProto());
    logger.info(response.toString());
    return PaymentResponse.copyFrom(response);
  }

  public InvoiceList listInvoices(long offset,long count,boolean pendingOnly,boolean reversed){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.ListInvoiceRequest.Builder requestBuilder = LightningOuterClass.ListInvoiceRequest.newBuilder();
    requestBuilder.setNumMaxInvoices(count);
    requestBuilder.setIndexOffset(offset);
    requestBuilder.setReversed(reversed);
    requestBuilder.setPendingOnly(pendingOnly);
    ListInvoiceResponse response=stub.listInvoices(requestBuilder.build());
    logger.info(response.toString());
    return InvoiceList.copyFrom(response);
  }

  public Invoice lookupInvoice(String rHash){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.PaymentHash.Builder requestBuilder = LightningOuterClass.PaymentHash.newBuilder();
    requestBuilder.setRHashStr(rHash);
    LightningOuterClass.Invoice invoice=stub.lookupInvoice(requestBuilder.build());
    logger.info(invoice.toString());
    return Invoice.copyFrom(invoice);
  }

  public PayReq decodePayReq(String requestString){
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.PayReqString.Builder requestBuilder = LightningOuterClass.PayReqString.newBuilder();
    requestBuilder.setPayReq(requestString);
    LightningOuterClass.PayReq payReq=stub.decodePayReq(requestBuilder.build());
    logger.info(payReq.toString());
    return PayReq.copyFrom(payReq);
  }
}
