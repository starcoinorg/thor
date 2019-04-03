package org.starcoin.lightning.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.InputStream;
import javax.net.ssl.SSLException;

public class Utils {

  public static Channel buildChannel(InputStream cert,String host,int port) throws SSLException {
    SslContext sslContext = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
        .trustManager(cert)
        .build();

    ManagedChannel channel = NettyChannelBuilder.forAddress(host, port)
        .sslContext(sslContext)
        .build();
    return channel;
  }
}
