package org.starcoin.lightning.client;

import io.grpc.Channel;
import java.io.InputStream;
import javax.net.ssl.SSLException;
import org.junit.*;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.Payment;

public class SyncClientTest {

  @Test
  public void testAddInvoice() throws SSLException {
    InputStream cert=this.getClass().getClassLoader().getResourceAsStream("alice.cert");
    Channel channel=Utils.buildChannel(cert,"starcoin-firstbox",30009);
    SyncClient client = new SyncClient(channel);
    String value="abc";
    Invoice invoice = new Invoice(HashUtils.hash160(value.getBytes()),20);

    AddInvoiceResponse addInvoiceResponse=client.addInvoice(invoice);

    InputStream cert1=this.getClass().getClassLoader().getResourceAsStream("bob.cert");
    Channel channel1=Utils.buildChannel(cert1,"starcoin-firstbox",40009);
    SyncClient client1 = new SyncClient(channel1);
    System.out.println(client1.sendPayment(new Payment(addInvoiceResponse.getPaymentRequest())));

  }

  @Test
  @Ignore
  public void testPay() throws SSLException {
    InputStream cert1=this.getClass().getClassLoader().getResourceAsStream("bob.cert");
    Channel channel1=Utils.buildChannel(cert1,"starcoin-firstbox",40009);
    SyncClient client1 = new SyncClient(channel1);
    System.out.println(client1.sendPayment(new Payment("lnsb200n1pw2fy3cpp5zager56e6wldw08pnupvem6t85r72wftjxsgyc074ydwu36rjx2sdqqcqzys9wqkyctnkd22kaq6nwn36kmg0ax4maulsywpa6875qpyd5perev3gcjz5g8trups7ya3nquat5pv8z88talhraz93xa2ttk92rgx4zqpend2gx")));
  }
}
