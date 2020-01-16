/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.example.https.helloworld;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.example.https.security.KeyStoreData;
import io.netty.example.https.util.ApplicationProperties;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * An HTTPS server that sends back the content of the received HTTP request in a pretty plaintext form.
 */
public final class HttpsHelloWorldServer {

    public static void main(String[] args) {
        Properties appProp = null;
        try {
            appProp = ApplicationProperties.from("src/main/resources/application.properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
        final int serverPort = Integer.valueOf(appProp.getProperty("server.port"));
        final String securityConfigFileName = appProp.getProperty("security.config.filepath");

        KeyStoreData keyStoreData = null;
        try {
            keyStoreData = KeyStoreData.from(securityConfigFileName);
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }

        SslContext sslCtx = null;
        try {
            sslCtx = SslContextBuilder
                    .forServer(keyStoreData.getKey(), keyStoreData.getCertificateChain())
                    .build();
        } catch (SSLException e) {
            e.printStackTrace();
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpsHelloWorldServerInitializer(sslCtx));

            Channel ch = b.bind(serverPort).sync().channel();
            System.err.println("Open your web browser and navigate to https://127.0.0.1:" + serverPort + '/');

            ch.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}