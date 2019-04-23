package org.starcoin.lightning.client.core;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.starcoin.lightning.client.HashUtils;
import org.starcoin.lightning.proto.LightningOuterClass;

public class Invoice {

  public enum InvoiceState {
    OPEN,
    SETTLED,
    CANCELED,
    ACCEPTED
  }
  private byte[] publicKey ;
  private String memo;
  private byte[] rPreimage;
  private byte[] rHash;
  private long value;
  private String paymentRequest;
  private byte[] descriptionHash;
  private long expiry;
  private String fallbackAddr;
  private InvoiceState state;

  public Invoice(){}

  public Invoice(byte[] rHash, long value) {
    this.rHash = rHash;
    this.value = value;
  }

  private Invoice(String memo, byte[] rPreimage, byte[] rHash, long value,
      String paymentRequest, byte[] descriptionHash, long expiry, String fallbackAddr,
      InvoiceState state) {
    this.memo = memo;
    this.rPreimage = rPreimage;
    this.rHash = rHash;
    this.value = value;
    this.paymentRequest = paymentRequest;
    this.descriptionHash = descriptionHash;
    this.expiry = expiry;
    this.fallbackAddr = fallbackAddr;
    this.state = state;
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

  public String getFallbackAddr() {
    return fallbackAddr;
  }

  public InvoiceState getState() {
    return state;
  }

  public boolean invoiceDone() {
      return Invoice.InvoiceState.SETTLED == this.state || Invoice.InvoiceState.CANCELED == this.state;
  }

  public void setMemo(String memo) {
    this.memo = memo;
  }

  public void setrPreimage(byte[] rPreimage) {
    this.rPreimage = rPreimage;
  }

  public void setrHash(byte[] rHash) {
    this.rHash = rHash;
  }

  public void setValue(long value) {
    this.value = value;
  }

  public void setPaymentRequest(String paymentRequest) {
    this.paymentRequest = paymentRequest;
  }

  public void setDescriptionHash(byte[] descriptionHash) {
    this.descriptionHash = descriptionHash;
  }

  public void setExpiry(long expiry) {
    this.expiry = expiry;
  }

  public void setFallbackAddr(String fallbackAddr) {
    this.fallbackAddr = fallbackAddr;
  }

  public void setState(InvoiceState state) {
    this.state = state;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public void setPublicKey(byte[] publicKey) {
    this.publicKey = publicKey;
  }

  public LightningOuterClass.Invoice toProto(){
    LightningOuterClass.Invoice.Builder invoiceBuilder = LightningOuterClass.Invoice.newBuilder();
    invoiceBuilder.setRHash(ByteString.copyFrom(this.getrHash()));
    invoiceBuilder.setValue(this.getValue());
    return invoiceBuilder.build();
  }

  public static Invoice copyFrom(LightningOuterClass.Invoice invoice){
    InvoiceState state=InvoiceState.OPEN;
    switch (invoice.getStateValue()){
      case LightningOuterClass.Invoice.InvoiceState.OPEN_VALUE:
        state=InvoiceState.OPEN;
        break;
      case LightningOuterClass.Invoice.InvoiceState.SETTLED_VALUE:
        state=InvoiceState.SETTLED;
        break;
      case LightningOuterClass.Invoice.InvoiceState.CANCELED_VALUE:
        state=InvoiceState.CANCELED;
        break;
      case LightningOuterClass.Invoice.InvoiceState.ACCEPTED_VALUE:
        state=InvoiceState.ACCEPTED;
        break;
    }
    return new Invoice(invoice.getMemo(),invoice.getRPreimage().toByteArray(),invoice.getRHash().toByteArray(),invoice.getValue(),
        invoice.getPaymentRequest(),invoice.getDescriptionHash().toByteArray(),invoice.getExpiry(),invoice.getFallbackAddr(),state);
  }

  @Override
  public String toString() {
    return "Invoice{" +
        "memo='" + memo + '\'' +
//        ", rPreimage=" + HashUtils.bytesToHex(rPreimage) +
        ", rHash=" + HashUtils.bytesToHex(rHash) +
        ", value=" + value +
        ", paymentRequest='" + paymentRequest + '\'' +
        ", descriptionHash=" + Arrays.toString(descriptionHash) +
        ", expiry=" + expiry +
        ", fallback_addr='" + fallbackAddr + '\'' +
        ", state=" + state +
        '}';
  }
}
