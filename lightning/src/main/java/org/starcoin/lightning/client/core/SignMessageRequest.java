package org.starcoin.lightning.client.core;

import com.google.protobuf.ByteString;
import org.starcoin.lightning.proto.LightningOuterClass;

public class SignMessageRequest {
  private byte[] msg;

  public SignMessageRequest(byte[] msg) {
    this.msg = msg;
  }

  public LightningOuterClass.SignMessageRequest toProto() {
    return LightningOuterClass.SignMessageRequest.newBuilder()
        .setMsg(ByteString.copyFrom(this.msg))
        .build();
  }
}
