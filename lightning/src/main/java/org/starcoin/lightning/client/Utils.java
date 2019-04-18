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
import java.io.InputStream;
import javax.net.ssl.SSLException;

public class Utils {

  private static final Metadata.Key<String> MACAROON_METADATA_KEY = Metadata.Key.of("macaroon", ASCII_STRING_MARSHALLER);

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

}
