package org.starcoin.lightning.client;

import io.grpc.Channel;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.starcoin.lightning.client.core.*;
import org.starcoin.lightning.proto.InvoicesGrpc;
import org.starcoin.lightning.proto.LightningGrpc;
import org.starcoin.lightning.proto.LightningOuterClass;
import org.starcoin.lightning.proto.LightningOuterClass.GetInfoRequest;
import org.starcoin.lightning.proto.LightningOuterClass.GetInfoResponse;
import org.starcoin.lightning.proto.LightningOuterClass.ListInvoiceResponse;
import org.starcoin.lightning.proto.LightningOuterClass.WalletBalanceRequest;
import org.starcoin.lightning.proto.LightningOuterClass.WalletBalanceResponse;

public class SyncClient {

  private Logger logger = Logger.getLogger(SyncClient.class.getName());
  private Channel channel;

  public SyncClient(Channel channel) {
    this.channel = channel;
  }

  public AddInvoiceResponse addInvoice(Invoice invoice) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.Invoice invoiceProto = invoice.toProto();
    logger.info(invoiceProto.toString());
    LightningOuterClass.AddInvoiceResponse response = stub.addInvoice(invoiceProto);
    logger.info(response.toString());
    return AddInvoiceResponse.copyFrom(response);
  }

  public PaymentResponse sendPayment(Payment payment) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.SendResponse response = stub.sendPaymentSync(payment.toProto());
    logger.info(response.toString());
    return PaymentResponse.copyFrom(response);
  }

  public InvoiceList listInvoices(long offset, long count, boolean pendingOnly, boolean reversed) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.ListInvoiceRequest.Builder requestBuilder =
        LightningOuterClass.ListInvoiceRequest.newBuilder();
    requestBuilder.setNumMaxInvoices(count);
    requestBuilder.setIndexOffset(offset);
    requestBuilder.setReversed(reversed);
    requestBuilder.setPendingOnly(pendingOnly);
    ListInvoiceResponse response = stub.listInvoices(requestBuilder.build());
    logger.info(response.toString());
    return InvoiceList.copyFrom(response);
  }

  public Invoice lookupInvoice(String rHash) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.PaymentHash.Builder requestBuilder =
        LightningOuterClass.PaymentHash.newBuilder();
    requestBuilder.setRHashStr(rHash);
    LightningOuterClass.Invoice invoice = stub.lookupInvoice(requestBuilder.build());
    logger.info(invoice.toString());
    return Invoice.copyFrom(invoice);
  }

  public PayReq decodePayReq(String requestString) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.PayReqString.Builder requestBuilder =
        LightningOuterClass.PayReqString.newBuilder();
    requestBuilder.setPayReq(requestString);
    LightningOuterClass.PayReq payReq = stub.decodePayReq(requestBuilder.build());
    logger.info(payReq.toString());
    return PayReq.copyFrom(payReq);
  }

  public List<ChannelResponse> listChannels(org.starcoin.lightning.client.core.Channel channel) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    return stub.listChannels(channel.toProto()).getChannelsList().stream()
        .map(c -> new ChannelResponse(c))
        .collect(Collectors.toList());
  }

  public String getIdentityPubkey() {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    GetInfoResponse resp = stub.getInfo(GetInfoRequest.newBuilder().build());
    return resp.getIdentityPubkey();
  }

  public void settleInvoice(SettleInvoiceRequest request) {
    InvoicesGrpc.InvoicesBlockingStub stub = InvoicesGrpc.newBlockingStub(this.channel);
    stub.settleInvoice(request.toProto());
  }

  public org.starcoin.lightning.client.core.WalletBalanceResponse walletBalance() {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    WalletBalanceResponse response = stub.walletBalance(WalletBalanceRequest.newBuilder().build());
    logger.info(response.toString());
    return org.starcoin.lightning.client.core.WalletBalanceResponse.copyFrom(response);
  }

  public SignMessageResponse signMessage(SignMessageRequest request) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    return SignMessageResponse.copyFrom(stub.signMessage(request.toProto()));
  }

  public VerifyMessageResponse verifyMessage(VerifyMessageRequest request) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    return VerifyMessageResponse.copyFrom(stub.verifyMessage(request.toProto()));
  }
}
