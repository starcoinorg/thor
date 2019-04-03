package org.starcoin.lightning.client.core;

import org.starcoin.lightning.proto.LightningOuterClass;

public class AddInvoiceResponse {

  private byte[] rHash;

  private String paymentRequest;

  private long addIndex;

  private AddInvoiceResponse(byte[] rHash, String paymentRequest, long addIndex) {
    this.rHash = rHash;
    this.paymentRequest = paymentRequest;
    this.addIndex = addIndex;
  }

  public static AddInvoiceResponse copyFrom(LightningOuterClass.AddInvoiceResponse response){
    return new AddInvoiceResponse(response.getRHash().toByteArray(),response.getPaymentRequest(),response.getAddIndex());
  }

}
