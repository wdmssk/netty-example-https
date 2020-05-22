package io.netty.example.https.util;

import io.vavr.control.Try;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

public final class ApplicationProperties {
    private ApplicationProperties() {
    }

    /**
     * Reads property list from the file.
     *
     * @param filePath the path of the property file
     * @return property list
     */
    public static Try<Properties> from(String filePath) {
        return Try.withResources(() -> FileChannel.open(Paths.get(filePath), StandardOpenOption.READ))
                  .of(fileChannel -> getProperties(fileChannel))
                  .onFailure(ex -> System.err
                          .println(ex.getClass().getName() + " error while loading the config file: " + filePath));
    }

    private static Properties getProperties(FileChannel fileChannel) throws IOException {
        Properties properties = new Properties();
        properties.load(Channels.newInputStream(fileChannel));
        return properties;
    }
}