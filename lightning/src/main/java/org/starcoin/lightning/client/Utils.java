package org.starcoin.lightning.client;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import java.io.File;
import javax.net.ssl.SSLException;

public class Utils {

  public static Channel buildChannel(String path) throws SSLException {
    File trustedServerCertificate = new File(path);
    SslContext sslContext = GrpcSslContexts.configure(SslContextBuilder.forClient(), SslProvider.OPENSSL)
        .trustManager(trustedServerCertificate)
        .build();

    ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", 10001)
        .sslContext(sslContext)
        .build();
    return channel;
  }
}
