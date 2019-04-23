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

    hash ="lnbc241pveeq09pp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdqqnp4q0n326hr8v9zprg8gsvezcch06gfaqqhde2aj730yg0durunfhv66jd3m5klcwhq68vdsmx2rjgxeay5v0tkt2v5sjaky4eqahe4fx3k9sqavvce3capfuwv8rvjng57jrtfajn5dkpqv8yelsewtljwmmycq62k443";
    invoice =Utils.decode(hash);
    System.out.println(invoice);
    System.out.println(HashUtils.bytesToHex(invoice.getPublicKey()));

    hash ="lntb1m1pdthhh0pp5063r8hu6f6hk7tpauhgvl3nnf4ur3xntcnhujcz5w82yq7nhjuysdq6d4ujqenfwfehggrfdemx76trv5xqrrss6uxhewtmjkumpr7w6prkgttku76azfq7l8cx9v74pcv85hzyvs9n23dhu9u354xcqpnzey45ua3g2m4dywuw7udrt2sdsvjf3rawdqcpas9mah";
    invoice =Utils.decode(hash);
    System.out.println(invoice);
    System.out.println(HashUtils.bytesToHex(invoice.getPublicKey()));

  }


}
