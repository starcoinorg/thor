package org.starcoin.lightning.client.core;

import java.util.ArrayList;
import java.util.List;
import org.starcoin.lightning.proto.LightningOuterClass;

public class InvoiceList {

  private List<Invoice> invoices;

  private long lastIndexOffset;

  private long firstIndexOffset;

  public List<Invoice> getInvoices() {
    return invoices;
  }

  public long getLastIndexOffset() {
    return lastIndexOffset;
  }

  public long getFirstIndexOffset() {
    return firstIndexOffset;
  }

  private InvoiceList(List<Invoice> invoices, long lastIndexOffset, long firstIndexOffset) {
    this.invoices = invoices;
    this.lastIndexOffset = lastIndexOffset;
    this.firstIndexOffset = firstIndexOffset;
  }

  public static InvoiceList copyFrom(LightningOuterClass.ListInvoiceResponse response){
    List<Invoice> invoices = new ArrayList<>();
    for(LightningOuterClass.Invoice invoice:response.getInvoicesList()){
      invoices.add(Invoice.copyFrom(invoice));
    }
    return new InvoiceList(invoices,response.getLastIndexOffset(),response.getFirstIndexOffset());
  }
}
