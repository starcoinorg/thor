package org.starcoin.lightning.client.core;

import com.google.protobuf.ByteString;
import org.starcoin.lightning.proto.InvoicesOuterClass.SettleInvoiceMsg;

public class SettleInvoiceRequest {
  private byte[] preimage;

  public byte[] getPreimage() {
    return preimage;
  }

  public SettleInvoiceRequest(byte[] preimage) {
    this.preimage = preimage;
  }

  public SettleInvoiceMsg toProto(){
    SettleInvoiceMsg.Builder builder = SettleInvoiceMsg.newBuilder();
    builder.setPreimage(ByteString.copyFrom(this.preimage));
    return builder.build();
  }
}
