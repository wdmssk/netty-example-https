package io.netty.https;

import io.netty.https.security.KeyStoreData;
import io.netty.https.util.ApplicationProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.vavr.control.Try;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;


@Command(name = "nettyHttps-1.0-SNAPSHOT-all.jar", mixinStandardHelpOptions = true, version = "1.0-SNAPSHOT",
        description = "An HTTPS server that sends back the content of the received HTTP request.")
public final class App implements Callable<Integer> {

    public static final String LOCAL_PORT = "local.port";
    public static final String SECURITY_CONFIG_FILEPATH = "security.config.filepath";

    @Option(names = { "-c", "--config" }, description = "Path of the configuration file.")
    private String configFilePath = "src/main/resources/application.properties";

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws InterruptedException {
        final Try<Properties> p = ApplicationProperties.from(configFilePath);
        if (p.isFailure()) {
            return 1;
        }

        final Try<Integer> localPort = getServerPort(p.get());
        if (localPort.isFailure()) {
            return 1;
        }

        Try<SslContext> sslContext = getSslContext(p.get());
        if (sslContext.isFailure()) {
            return 1;
        }

        HttpsServer.start(sslContext.get(), localPort.get());
        return 0;
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
        return ApplicationProperties.from(securityConfFilePath)
                                    .flatMap(securityConfigProp -> Try.of(() -> KeyStoreData.from(securityConfigProp))
                                                                      .onFailure(ex -> System.err.println(
                                                                              "KeyStore data read failed, Error message: " +
                                                                              ex.getMessage())));
    }

    private static Try<SslContext> getSslContext(KeyStoreData keyStoreData) {
        return Try.of(() -> SslContextBuilder.forServer(keyStoreData.getKey(),
                                                        keyStoreData.getCertificateChain())
                                             .build())
                  .onFailure(
                          ex -> System.err.println("SslContext creation failed, Error message: " + ex.getMessage()));
    }
}