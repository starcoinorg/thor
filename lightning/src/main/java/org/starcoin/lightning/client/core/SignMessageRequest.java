package org.starcoin.lightning.client.core;

import com.google.protobuf.ByteString;
import org.starcoin.lightning.proto.SignOuterClass;

public class SignMessageRequest {
  private byte[] msg;

  public SignMessageRequest(byte[] msg) {
    this.msg = msg;
  }

  public SignOuterClass.SignMessageRequest toProto() {
    return SignOuterClass.SignMessageRequest.newBuilder()
        .setMsg(ByteString.copyFrom(this.msg))
        .build();
  }
}
