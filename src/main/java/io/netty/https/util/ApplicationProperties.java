package io.netty.https.util;

import io.vavr.control.Try;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ApplicationProperties {
    private ApplicationProperties() {
    }

    /**
     * Reads property list from the file.
     *
     * @param clazz        class from which the file is loaded
     * @param resourecPath name of the desired resource
     * @return property list
     */
    public static Try<Properties> from(Class<?> clazz, String resourecPath) {
        return Try.withResources(() -> clazz.getResourceAsStream(resourecPath))
                  .of(inputStream -> getProperties(inputStream))
                  .onFailure(ex -> System.err
                          .println(ex.getClass().getName() + " error while loading the config file: " + resourecPath));
    }

    private static Properties getProperties(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

}