package org.starcoin.lightning.client.core;

import org.starcoin.lightning.proto.LightningOuterClass;
import org.starcoin.lightning.proto.LightningOuterClass.ListChannelsRequest;

public class Channel {

  private boolean activeOnly;
  private boolean inactiveOnly;
  private boolean publicOnly;
  private boolean privateOnly;

  public Channel(boolean activeOnly, boolean inactiveOnly, boolean publicOnly,
      boolean privateOnly) {
    this.activeOnly = activeOnly;
    this.inactiveOnly = inactiveOnly;
    this.publicOnly = publicOnly;
    this.privateOnly = privateOnly;
  }

  public LightningOuterClass.ListChannelsRequest toProto() {
    ListChannelsRequest listChannelsRequest = ListChannelsRequest.newBuilder()
        .setActiveOnly(this.activeOnly).setInactiveOnly(this.inactiveOnly)
        .setPublicOnly(this.publicOnly).setPrivateOnly(this.privateOnly).build();
    return listChannelsRequest;
  }
}
