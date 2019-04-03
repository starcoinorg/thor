package org.starcoin.lightning.client.core;

public class Invoice {

  public enum InvoiceState {
    OPEN,
    SETTLED,
    CANCELED,
    ACCEPTED
  }

  private String memo;
  private byte[] receipt;
  private byte[] rPreimage;
  private byte[] rHash;
  private long value;
  private String paymentRequest;
  private byte[] descriptionHash;
  private long expiry;
  private String fallback_addr;
  private InvoiceState state;

  public Invoice(byte[] receipt, byte[] rHash, long value) {
    this.receipt = receipt;
    this.rPreimage = rPreimage;
    this.rHash = rHash;
    this.value = value;
  }

  public String getMemo() {
    return memo;
  }

  public byte[] getReceipt() {
    return receipt;
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
}
