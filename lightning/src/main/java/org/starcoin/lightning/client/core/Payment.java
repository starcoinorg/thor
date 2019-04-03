package org.starcoin.lightning.client.core;

import org.starcoin.lightning.proto.LightningOuterClass;

public class Payment {
  private String paymentRequest;

  public Payment(String paymentRequest) {
    this.paymentRequest = paymentRequest;
  }

  public String getPaymentRequest() {
    return paymentRequest;
  }

  public LightningOuterClass.SendRequest toProto(){
    LightningOuterClass.SendRequest.Builder requestBuilder = LightningOuterClass.SendRequest.newBuilder();
    requestBuilder.setPaymentRequest(this.getPaymentRequest());
    return requestBuilder.build();
  }

}
