package org.starcoin.lightning.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.starcoin.lightning.client.core.*;
import org.starcoin.lightning.proto.InvoicesGrpc;
import org.starcoin.lightning.proto.InvoicesOuterClass;
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

  private int deadlineMs = 60 * 1000;

  public SyncClient(Channel channel) {
    this.channel = channel;
  }

  public AddInvoiceResponse addInvoice(Invoice invoice) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.Invoice invoiceProto = invoice.toProto();
    logger.info(invoiceProto.toString());
    LightningOuterClass.AddInvoiceResponse response =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).addInvoice(invoiceProto);
    logger.info(response.toString());
    return AddInvoiceResponse.copyFrom(response);
  }

  public PaymentResponse sendPayment(Payment payment) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.SendResponse response =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .sendPaymentSync(payment.toProto());
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
    ListInvoiceResponse response =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .listInvoices(requestBuilder.build());
    logger.info(response.toString());
    return InvoiceList.copyFrom(response);
  }

  public Invoice lookupInvoice(String rHash) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.PaymentHash.Builder requestBuilder =
        LightningOuterClass.PaymentHash.newBuilder();
    requestBuilder.setRHashStr(rHash);
    LightningOuterClass.Invoice invoice =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .lookupInvoice(requestBuilder.build());
    logger.info(invoice.toString());
    return Invoice.copyFrom(invoice);
  }

  public PayReq decodePayReq(String requestString) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(channel);
    LightningOuterClass.PayReqString.Builder requestBuilder =
        LightningOuterClass.PayReqString.newBuilder();
    requestBuilder.setPayReq(requestString);
    LightningOuterClass.PayReq payReq =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .decodePayReq(requestBuilder.build());
    logger.info(payReq.toString());
    return PayReq.copyFrom(payReq);
  }

  public List<ChannelResponse> listChannels(org.starcoin.lightning.client.core.Channel channel) {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).listChannels(channel.toProto())
        .getChannelsList().stream()
        .map(c -> new ChannelResponse(c))
        .collect(Collectors.toList());
  }

  public String getIdentityPubkey() {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    GetInfoResponse resp =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .getInfo(GetInfoRequest.newBuilder().build());
    return resp.getIdentityPubkey();
  }

  public void settleInvoice(SettleInvoiceRequest request) {
    InvoicesGrpc.InvoicesBlockingStub stub = InvoicesGrpc.newBlockingStub(this.channel);
    logger.info(
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .settleInvoice(request.toProto())
            .toString());
  }

  public void cancelInvoice(byte[] paymentHash) {
    InvoicesGrpc.InvoicesBlockingStub stub = InvoicesGrpc.newBlockingStub(this.channel);
    InvoicesOuterClass.CancelInvoiceMsg.Builder builder =
        InvoicesOuterClass.CancelInvoiceMsg.newBuilder();
    builder.setPaymentHash(ByteString.copyFrom(paymentHash));
    InvoicesOuterClass.CancelInvoiceResp resp =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).cancelInvoice(builder.build());
    logger.info(resp.toString());
  }

  public org.starcoin.lightning.client.core.WalletBalanceResponse walletBalance() {
    LightningGrpc.LightningBlockingStub stub = LightningGrpc.newBlockingStub(this.channel);
    WalletBalanceResponse response =
        stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
            .walletBalance(WalletBalanceRequest.newBuilder().build());
    logger.info(response.toString());
    return org.starcoin.lightning.client.core.WalletBalanceResponse.copyFrom(response);
  }
}
