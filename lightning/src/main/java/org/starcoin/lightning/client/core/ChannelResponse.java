package org.starcoin.lightning.client.core;

public class ChannelResponse {

  public ChannelResponse(org.starcoin.lightning.proto.LightningOuterClass.Channel channel) {
    active = channel.getActive();
    remotePubkey = channel.getRemotePubkey();
    channelPoint = channel.getChannelPoint();
    chanId = channel.getChanId();
    capacity = channel.getCapacity();
    localBanance = channel.getLocalBalance();
    remoteBalance = channel.getRemoteBalance();
    commitFee = channel.getCommitFee();
    commitWeight = channel.getCommitWeight();
    feePerKw = channel.getFeePerKw();
    unsettledBalance = channel.getUnsettledBalance();
    totalSatoshisSent = channel.getTotalSatoshisSent();
    totalSatoshisReceived = channel.getTotalSatoshisReceived();
    numUpdates = channel.getNumUpdates();
    csvDelay = channel.getCsvDelay();
    initiator = channel.getInitiator();
    pendingHtlcs = channel.getPendingHtlcsOrBuilderList().toString();
  }

  public boolean isActive() {
    return active;
  }

  public String getRemotePubkey() {
    return remotePubkey;
  }

  public String getChannelPoint() {
    return channelPoint;
  }

  public long getChanId() {
    return chanId;
  }

  public long getCapacity() {
    return capacity;
  }

  public long getLocalBanance() {
    return localBanance;
  }

  public long getRemoteBalance() {
    return remoteBalance;
  }

  public long getCommitFee() {
    return commitFee;
  }

  public long getCommitWeight() {
    return commitWeight;
  }

  public long getFeePerKw() {
    return feePerKw;
  }

  public long getUnsettledBalance() {
    return unsettledBalance;
  }

  public long getTotalSatoshisSent() {
    return totalSatoshisSent;
  }

  public long getTotalSatoshisReceived() {
    return totalSatoshisReceived;
  }

  public long getNumUpdates() {
    return numUpdates;
  }

  public long getCsvDelay() {
    return csvDelay;
  }

  public boolean isInitiator() {
    return initiator;
  }

  public String getPendingHtlcs() {
    return pendingHtlcs;
  }

  private boolean active;
  private String remotePubkey;
  private String channelPoint;
  private long chanId;
  private long capacity;
  private long localBanance;
  private long remoteBalance;
  private long commitFee;
  private long commitWeight;
  private long feePerKw;
  private long unsettledBalance;
  private long totalSatoshisSent;
  private long totalSatoshisReceived;
  private long numUpdates;
  private long csvDelay;
  private boolean initiator;
  private String pendingHtlcs;

  @Override
  public String toString() {
    return "ChannelResponse{" +
        "active=" + active +
        ", remotePubkey='" + remotePubkey + '\'' +
        ", channelPoint='" + channelPoint + '\'' +
        ", chanId=" + chanId +
        ", capacity=" + capacity +
        ", localBanance=" + localBanance +
        ", remoteBalance=" + remoteBalance +
        ", commitFee=" + commitFee +
        ", commitWeight=" + commitWeight +
        ", feePerKw=" + feePerKw +
        ", unsettledBalance=" + unsettledBalance +
        ", totalSatoshisSent=" + totalSatoshisSent +
        ", totalSatoshisReceived=" + totalSatoshisReceived +
        ", numUpdates=" + numUpdates +
        ", csvDelay=" + csvDelay +
        ", initiator=" + initiator +
        ", pendingHtlcs='" + pendingHtlcs + '\'' +
        '}';
  }

}
