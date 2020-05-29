package io.netty.https;

import io.netty.https.security.KeyStoreData;
import io.netty.https.util.ApplicationProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.vavr.control.Try;

import java.util.Objects;
import java.util.Properties;


public final class App {
    private static final String CONFIG_FILEPATH = "/application.properties";

    public static final String LOCAL_PORT = "local.port";
    public static final String SECURITY_CONFIG_FILEPATH = "security.config.filepath";

    public static void main(String[] args) throws InterruptedException {
        final Try<Properties> p = ApplicationProperties.from(App.class, CONFIG_FILEPATH);
        if (p.isFailure()) {
            System.exit(1);
        }

        final Try<Integer> localPort = getServerPort(p.get());
        if (localPort.isFailure()) {
            System.exit(1);
        }

        Try<SslContext> sslContext = getSslContext(p.get());
        if (sslContext.isFailure()) {
            System.exit(1);
        }

        HttpsServer.start(sslContext.get(), localPort.get());
    }

    private static Try<Integer> getServerPort(Properties properties) {
        return Try.of(() -> Objects.requireNonNull(properties.getProperty(LOCAL_PORT)))
                  .onFailure(ex -> System.err.println("Failed to retrieve \"server.port\" from properties file."))
                  .flatMap(str -> Try.of(() -> Integer.valueOf(str))
                                     .onFailure(ex -> System.err.println("Failed to convert \"" + str + "\" to int."))
                  );
    }

    private static Try<SslContext> getSslContext(Properties properties) {
        return Try.of(() -> Objects.requireNonNull(properties.getProperty(SECURITY_CONFIG_FILEPATH)))
                  .onFailure(ex -> System.err
                          .println("Failed to retrieve \"security.config.filepath\" from properties file."))
                  .flatMap(securityConfigFileName -> getKeyStoreData(securityConfigFileName))
                  .flatMap(keyStoreData -> getSslContext(keyStoreData));
    }

    private static Try<KeyStoreData> getKeyStoreData(String securityConfFilePath) {
        return ApplicationProperties.from(App.class, securityConfFilePath)
                                    .flatMap(securityConfigProp -> Try.of(() -> KeyStoreData.from(securityConfigProp))
                                                                      .onFailure(ex -> System.err.println(
                                                                              "KeyStore data read failed. Error message: " +
                                                                              ex.getMessage())));
    }

    private static Try<SslContext> getSslContext(KeyStoreData keyStoreData) {
        return Try.of(() -> SslContextBuilder.forServer(keyStoreData.getKey(),
                                                        keyStoreData.getCertificateChain())
                                             .build())
                  .onFailure(
                          ex -> System.err.println("SslContext creation failed. Error message: " + ex.getMessage()));
    }
}