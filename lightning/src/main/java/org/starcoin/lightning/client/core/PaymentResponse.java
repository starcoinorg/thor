package org.starcoin.lightning.client.core;

import org.starcoin.lightning.client.HashUtils;
import org.starcoin.lightning.proto.LightningOuterClass;

public class PaymentResponse {

  private String paymentError;
  private byte[] paymentPreimage;
  private byte[] paymentHash;

  public String getPaymentError() {
    return paymentError;
  }

  public byte[] getPaymentPreimage() {
    return paymentPreimage;
  }

  public byte[] getPaymentHash() {
    return paymentHash;
  }

  private PaymentResponse(String paymentError, byte[] paymentHash,byte[] paymentPreimage) {
    this.paymentError = paymentError;
    this.paymentHash = paymentHash;
    this.paymentPreimage = paymentPreimage;
  }

  public static PaymentResponse copyFrom(LightningOuterClass.SendResponse sendResponse){
    return new PaymentResponse(sendResponse.getPaymentError(),sendResponse.getPaymentHash().toByteArray(),sendResponse.getPaymentPreimage().toByteArray());
  }

  @Override
  public String toString() {
    return "PaymentResponse{" +
        "paymentError='" + paymentError + '\'' +
        ", paymentPreimage=" + HashUtils.bytesToHex(paymentPreimage) +
        ", paymentHash=" + HashUtils.bytesToHex(paymentHash) +
        '}';
  }
}
