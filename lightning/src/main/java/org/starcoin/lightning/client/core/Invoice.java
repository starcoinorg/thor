package org.starcoin.lightning.client.core;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.starcoin.lightning.proto.LightningOuterClass;

public class Invoice {

  public enum InvoiceState {
    OPEN,
    SETTLED,
    CANCELED,
    ACCEPTED
  }

  private String memo;
  private byte[] rPreimage;
  private byte[] rHash;
  private long value;
  private String paymentRequest;
  private byte[] descriptionHash;
  private long expiry;
  private String fallback_addr;
  private InvoiceState state;

  public Invoice(byte[] rHash, long value) {
    this.rPreimage = rPreimage;
    this.rHash = rHash;
    this.value = value;
  }

  public String getMemo() {
    return memo;
  }

  public byte[] getrPreimage() {
    return rPreimage;
  }

  public byte[] getrHash() {
    return rHash;
  }

  public long getValue() {
    return value;
  }

  public String getPaymentRequest() {
    return paymentRequest;
  }

  public byte[] getDescriptionHash() {
    return descriptionHash;
  }

  public long getExpiry() {
    return expiry;
  }

  public String getFallback_addr() {
    return fallback_addr;
  }

  public InvoiceState getState() {
    return state;
  }

  public LightningOuterClass.Invoice toProto(){
    LightningOuterClass.Invoice.Builder invoiceBuilder = LightningOuterClass.Invoice.newBuilder();
    invoiceBuilder.setRHash(ByteString.copyFrom(this.getrHash()));
    invoiceBuilder.setValue(this.getValue());
    return invoiceBuilder.build();
  }

  @Override
  public String toString() {
    return "Invoice{" +
        "memo='" + memo + '\'' +
        ", rPreimage=" + Arrays.toString(rPreimage) +
        ", rHash=" + Arrays.toString(rHash) +
        ", value=" + value +
        ", paymentRequest='" + paymentRequest + '\'' +
        ", descriptionHash=" + Arrays.toString(descriptionHash) +
        ", expiry=" + expiry +
        ", fallback_addr='" + fallback_addr + '\'' +
        ", state=" + state +
        '}';
  }
}
