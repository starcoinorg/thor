package org.starcoin.lightning.client.core;

import org.starcoin.lightning.proto.LightningOuterClass;

public class WalletBalanceResponse {
  private long totalBalance;

  private long confirmedBalance;

  private long unconfirmedBalance;

  private WalletBalanceResponse(long totalBalance, long confirmedBalance, long unconfirmedBalance) {
    this.totalBalance = totalBalance;
    this.confirmedBalance = confirmedBalance;
    this.unconfirmedBalance = unconfirmedBalance;
  }

  public long getTotalBalance() {
    return totalBalance;
  }

  public long getConfirmedBalance() {
    return confirmedBalance;
  }

  public long getUnconfirmedBalance() {
    return unconfirmedBalance;
  }

  public static WalletBalanceResponse copyFrom(LightningOuterClass.WalletBalanceResponse response){
    return new WalletBalanceResponse(response.getTotalBalance(),response.getConfirmedBalance(),response.getUnconfirmedBalance());
  }
}
