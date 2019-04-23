package org.starcoin.lightning.client;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLException;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.ECKey.ECDSASignature;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SignatureDecodeException;
import org.starcoin.lightning.client.Bech32.Bech32Data;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.PayReq;

public class Utils {

  private static final Metadata.Key<String> MACAROON_METADATA_KEY = Metadata.Key.of("macaroon", ASCII_STRING_MARSHALLER);

  private static final Set<String> bech32HRPSegwitSet = new HashSet(){{
    add("bc");
    add("tb");
    add("tltc");
    add("ltc");
    add("sb");
  }};

  private static final long	mSatPerBtc = 100000000000l;

  private static final int signatureBase32Len = 104;

  private static final int timestampBase32Len = 7;

  // fieldTypeP is the field containing the payment hash.
  private static final byte fieldTypeP = 1;

  // fieldTypeD contains a short description of the payment.
  private static final byte fieldTypeD = 13;

  // fieldTypeN contains the pubkey of the target node.
  private static final byte fieldTypeN = 19;

  // fieldTypeH contains the hash of a description of the payment.
  private static final byte fieldTypeH = 23;

  // fieldTypeX contains the expiry in seconds of the invoice.
  private static final byte fieldTypeX = 6;

  // fieldTypeF contains a fallback on-chain address.
  private static final byte fieldTypeF = 9;

  // fieldTypeR contains extra routing information.
  private static final byte fieldTypeR = 3;

  // fieldTypeC contains an optional requested final CLTV delta.
  private static final byte fieldTypeC = 24;


  private static final int hashBase32Len = 52;
  private static final int pubKeyBase32Len = 53;

  public static Channel buildChannel(InputStream cert,String host,int port) throws SSLException {
    SslContext sslContext = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
        .trustManager(cert)
        .build();

    ManagedChannel channel = NettyChannelBuilder.forAddress(host, port)
        .sslContext(sslContext)
        .build();
    return channel;
  }

  public static Channel buildChannel(InputStream cert,String macaroon,String host,int port) throws SSLException {
    SslContext sslContext = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
        .trustManager(cert)
        .build();

    ManagedChannel channel = NettyChannelBuilder.forAddress(host, port)
        .sslContext(sslContext)
        .intercept(new io.grpc.ClientInterceptor(){
          @Override
          public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
              @Override
              public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(MACAROON_METADATA_KEY, macaroon);
                super.start(responseListener, headers);
              }
            };
          }
        })
        .build();
    return channel;
  }

  public static Invoice decode(String hash) throws IOException, SignatureDecodeException {
    Bech32Data data=Bech32.decode(hash);
    if(data.hrp.length()<3){
      throw new IllegalArgumentException("hrp too short");
    }
    if(!data.hrp.startsWith("ln")){
      throw new IllegalArgumentException("prefix should be \"ln\"");
    }
    boolean forActiveNetwork = false;
    String bech32HRPSegwit = null;
    for(String prefix:bech32HRPSegwitSet) {
      if(data.hrp.substring(2).startsWith(prefix)){
        bech32HRPSegwit= prefix;
        forActiveNetwork=true;
        break;
      }
    }
    if(forActiveNetwork==false){
      throw new IllegalArgumentException("invoice not for current active network");
    }

    int netPrefixLength = bech32HRPSegwit.length() + 2;
    long value=0;
    if(data.hrp.length()>netPrefixLength){
      String amount=data.hrp.substring(netPrefixLength);
      value=decodeAmount(amount);
    }

    byte[] invoiceData = Arrays.copyOfRange(data.data,0,data.data.length-signatureBase32Len);

    Invoice invoice = parseData(invoiceData);
    invoice.setValue(value);

    byte[] sigBase32 = Arrays.copyOfRange(data.data,data.data.length-signatureBase32Len,data.data.length);
    byte[] sigBase256 = Bech32.convertBits(sigBase32, 5, 8, true);
    byte recoveryID = sigBase256[64];

    byte[] taggedDataBytes= Bech32.convertBits(invoiceData, 5, 8, true);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    stream.write(data.hrp.getBytes());
    stream.write(taggedDataBytes);
    byte[] hashSign = HashUtils.sha256(stream.toByteArray());
    if(invoice.getPublicKey()==null){
      int headerByte = recoveryID + 27 + 4;

      ECDSASignature signature = new ECDSASignature();
      ECKey key=ECKey.recoverFromSignature(recoveryID,signature, Sha256Hash.of(hashSign),true);
      invoice.setPublicKey(key.getPubKey());
    }

    return invoice;
  }

  private static Invoice parseData(byte[] data){
    if(data.length<timestampBase32Len){
      throw new IllegalArgumentException("data too short");
    }
    byte[] timeStampBytes = Arrays.copyOfRange(data,0,timestampBase32Len);
    if(timeStampBytes.length!=timestampBase32Len){
      throw new IllegalArgumentException("timestamp must be 35 bits");
    }
    long timeStamp = base32ToLong(timeStampBytes);
    byte[] tagData = Arrays.copyOfRange(data,timestampBase32Len,data.length);
    int index = 0;
    Invoice invoice =new Invoice();
    while (true){
      if (tagData.length-index < 3 ){
        break;
      }

      byte typ = tagData[index];
      int dataLength=parseFieldDataLength(Arrays.copyOfRange(tagData,index+1,index+3));
      System.out.println("data len is "+dataLength);
      if (tagData.length < index+3+dataLength) {
        throw new IllegalArgumentException("invalid field length");
      }

      byte[] base32Data = Arrays.copyOfRange(tagData,index+3, index+3+dataLength);

      // Advance the index in preparation for the next iteration.
      index += 3 + dataLength;
      switch (typ){
        case fieldTypeP:
          invoice.setrHash(parsePaymentHash(base32Data));
          break;
        case fieldTypeN:
          invoice.setPublicKey(parseDestination(base32Data));
          break;
        case fieldTypeX:
          invoice.setExpiry(parseExpiry(base32Data));
          break;
      }

    }
    return invoice;
  }

  private static long parseExpiry(byte[] data){
    long expiry=base32ToLong(data);
    System.out.println(data);
    return expiry;
  }

  private static byte[] parseDestination(byte[] data){
    System.out.println(Arrays.toString(data));
    if(data.length<pubKeyBase32Len){
      throw new IllegalArgumentException("public key should be length of "+pubKeyBase32Len);
    }
    return Bech32.convertBits(data,5,8,false);
  }

  private static byte[] parsePaymentHash(byte[] data) {
    if(data.length!=hashBase32Len){
      throw new IllegalArgumentException("hash length must be "+hashBase32Len);
    }
    byte[] hash = Bech32.convertBits(data,5,8,false);
    return hash;
  }

  private static int parseFieldDataLength(byte[] data){
    if (data.length != 2) {
      throw new IllegalArgumentException("data length must be 2 bytes");
    }

    return data[0]<<5 |data[1];
  }

  private static long base32ToLong(byte[] data){
    if(data.length>13){
      throw new IllegalArgumentException("cannot parse data of length "+data.length + " to long");
    }
    long val = 0l;
    for( int i = 0; i < data.length; i++ ){
      val = val<<5 | (data[i]);
    }
    return val;
  }

  private static long decodeAmount(String amount){
    if(amount.length()<1){
      throw new IllegalArgumentException("amount must be non-empty");
    }


    char lastChar = amount.charAt(amount.length()-1);
    int digit=lastChar-'0';
    if(digit>=0&&digit<=9){
      long btc=Long.valueOf(amount);
      return btc*mSatPerBtc;
    }

    long num = Long.valueOf(amount.substring(0,amount.length()-1));
    long amountNum =0;
    switch (lastChar){
      case 'm':
        amountNum= num*100000000;
        break;
      case 'u':
        amountNum = num*100000;
        break;
      case 'n':
        amountNum = num *100;
        break;
      case 'p':
        if(num<10){
          throw new IllegalArgumentException("minimum amount is 10p");
        }
        if(num%10!=0){
          throw new IllegalArgumentException("amount "+num+" pBTC not expressible in msat");
        }
        amountNum = num%10;
        break;
    }
    return amountNum;
  }
}
