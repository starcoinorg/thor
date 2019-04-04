package org.starcoin.lightning.client.core;

import org.starcoin.lightning.proto.LightningOuterClass;

public class PayReq {

  private  String destination;
  private  String paymentHash;
  private  long numSatoshis;
  private  long timestamp;
  private  long expiry;
  private String description;

  private PayReq(String destination, String paymentHash, long numSatoshis, long timestamp,
      long expiry,
      String description) {
    this.destination = destination;
    this.paymentHash = paymentHash;
    this.numSatoshis = numSatoshis;
    this.timestamp = timestamp;
    this.expiry = expiry;
    this.description = description;
  }

  public String getDestination() {
    return destination;
  }

  public String getPaymentHash() {
    return paymentHash;
  }

  public long getNumSatoshis() {
    return numSatoshis;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getExpiry() {
    return expiry;
  }

  public String getDescription() {
    return description;
  }

  public static PayReq copyFrom(LightningOuterClass.PayReq req){
    return new PayReq(req.getDestination(),req.getPaymentHash(),req.getNumSatoshis(),req.getTimestamp(),req.getExpiry(),req.getDescription());
  }
}
