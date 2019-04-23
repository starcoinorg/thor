package org.starcoin.lightning.client;

import java.io.IOException;
import org.bitcoinj.core.SignatureDecodeException;
import org.junit.Test;
import org.starcoin.lightning.client.core.Invoice;

public class UtilsTest {

  @Test
  public void testDecode() throws IOException, SignatureDecodeException {
    String hash ="lnltc241pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqhp58yjmdan79s6qqdhdzgynm4zwqd5d7xmw5fk98klysy043l2ahrqsnp4q0n326hr8v9zprg8gsvezcch06gfaqqhde2aj730yg0durunfhv66859t2d55efrxdlgqg9hdqskfstdmyssdw4fjc8qdl522ct885pqk7acn2aczh0jeht0xhuhnkmm3h0qsrxedlwm9x86787zzn4qwwwcpjkl3t2";
    Invoice invoice =Utils.decode(hash);
    System.out.println(invoice);
    System.out.println(HashUtils.bytesToHex(invoice.getPublicKey()));

    hash ="lnsb2u1pwt6u20pp5ug69a6sc8cqlu6xt5vhx84jlg3yj6ypf8mzpe9665k8j6dketm6sdqqcqzpgejzjas3wv2hzxfzdnnnlshqfykaylq839fm92qcxhpsw5zhshwqhjgqvynchjnjmuyg0qlyavpxelr8g5plx735zgvlk2ju39z08ccqqfjds7j";
    invoice =Utils.decode(hash);
    System.out.println(invoice);
    System.out.println(HashUtils.bytesToHex(invoice.getPublicKey()));

  }


}
