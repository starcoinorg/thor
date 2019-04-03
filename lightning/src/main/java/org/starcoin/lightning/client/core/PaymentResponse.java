package org.starcoin.lightning.client.core;

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

  private PaymentResponse(String paymentError, byte[] paymentHash) {
    this.paymentError = paymentError;
    this.paymentHash = paymentHash;
  }

  public static PaymentResponse copyFrom(LightningOuterClass.SendResponse sendResponse){
    return new PaymentResponse(sendResponse.getPaymentError(),sendResponse.getPaymentHash().toByteArray());
  }
}
