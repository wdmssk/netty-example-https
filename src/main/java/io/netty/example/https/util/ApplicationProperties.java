package io.netty.example.https.util;

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
     * Returns property list read from the file defining the properties.
     *
     * @param filePath The path of the file withe the property list
     * @return property list with the properties defined in the file
     * @throws IOException if an error occurred when reading from the file
     */
    public static Properties from(String filePath) throws IOException {

        Properties properties = new Properties();
        try (final FileChannel fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ)) {
            properties.load(Channels.newInputStream(fileChannel));
        }

        return properties;
    }
}
