package org.starcoin.lightning.client;

import static junit.framework.TestCase.assertTrue;

import io.grpc.Channel;
import java.io.InputStream;
import javax.net.ssl.SSLException;
import org.junit.*;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.InvoiceList;
import org.starcoin.lightning.client.core.PayReq;

public class SyncClientTest {

  @Test
  public void testAddInvoice() throws SSLException {
    InputStream cert = this.getClass().getClassLoader().getResourceAsStream("alice.cert");
    Channel channel = Utils.buildChannel(cert, "starcoin-firstbox", 30009);
    SyncClient client = new SyncClient(channel);
    String value = "abc";
    byte[] hash = HashUtils.hash160(value.getBytes());
    Invoice invoice = new Invoice(hash, 20);
    AddInvoiceResponse addInvoiceResponse = client.addInvoice(invoice);
    PayReq req = client.decodePayReq(addInvoiceResponse.getPaymentRequest());
    assertTrue(req.getNumSatoshis() == 20);
    Invoice invoiceLookuped = client.lookupInvoice(req.getPaymentHash());
    assertTrue(invoiceLookuped != null);
    InvoiceList invoiceList = client.listInvoices(0, 20, false, false);
    assertTrue(invoiceList.getInvoices().size() > 1);
  }

  @Test
  public void testChannel() throws SSLException {
    InputStream bob = this.getClass().getClassLoader().getResourceAsStream("bob.cert");
    Channel channel = Utils.buildChannel(bob, "starcoin-firstbox", 40009);
    SyncClient client = new SyncClient(channel);
    org.starcoin.lightning.client.core.Channel lightningChannel = new org.starcoin.lightning.client.core.Channel(
        true, false, true, false);
    Assert.assertTrue(client.listChannels(lightningChannel).size() != 0);
  }
}
