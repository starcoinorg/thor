package org.starcoin.lightning.client;

import static junit.framework.TestCase.assertTrue;

import io.grpc.Channel;
import java.io.InputStream;
import javax.net.ssl.SSLException;
import org.junit.*;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.Invoice.InvoiceState;
import org.starcoin.lightning.client.core.InvoiceList;
import org.starcoin.lightning.client.core.PayReq;
import org.starcoin.lightning.client.core.Payment;
import org.starcoin.lightning.client.core.PaymentResponse;

public class SyncClientTest {

  SyncClient aliceCli;
  SyncClient bobCli;

  public SyncClientTest() throws SSLException {
    aliceCli = gencli("alice.cert", 30009);
    bobCli = gencli("bob.cert", 40009);
  }

  @Test
  public void testAddInvoice() throws SSLException {
    SyncClient client = aliceCli;
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
    SyncClient client = bobCli;
    org.starcoin.lightning.client.core.Channel lightningChannel = new org.starcoin.lightning.client.core.Channel(
        true, false, true, false);
    Assert.assertTrue(client.listChannels(lightningChannel).size() != 0);
  }

  @Test
  public void testSendPayment() throws SSLException {
    AddInvoiceResponse invoice = aliceCli
        .addInvoice(new Invoice(HashUtils.hash160("starcoin".getBytes()), 20));
    PaymentResponse paymentResponse = bobCli.sendPayment(new Payment(invoice.getPaymentRequest()));
    Assert.assertEquals("", paymentResponse.getPaymentError());
    Invoice findInvoice = aliceCli
        .lookupInvoice(HashUtils.bytesToHex(paymentResponse.getPaymentHash()));
    Assert.assertEquals(InvoiceState.SETTLED, findInvoice.getState());
  }

  @Test
  public void testGetIdentityPubkey() throws SSLException {
    String identityPubkey = bobCli.getIdentityPubkey();
    Assert.assertEquals(
        "032cc5d4af1e054bec47fc995bafa99c8aeeef36aeb60fe5a354e59b8c79079f14",
        identityPubkey);
  }

  private SyncClient gencli(String certPath, int port) throws SSLException {
    String peer = "starcoin-firstbox";
    InputStream cert = getClass().getClassLoader().getResourceAsStream(certPath);
    Channel channel = Utils.buildChannel(cert, peer, port);
    SyncClient client = new SyncClient(channel);
    return client;
  }
}
