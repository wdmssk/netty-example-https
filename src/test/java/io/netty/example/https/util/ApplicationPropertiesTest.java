package io.netty.example.https.util;

import org.junit.Test;

import java.io.IOException;
import java.security.Security;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

public class ApplicationPropertiesTest {
    @Test
    public void test_from_simpleProperties() throws IOException {
        Properties prop = ApplicationProperties.from("src/test/resources/application.properties");
        assertEquals("8443", prop.getProperty("server.port"));
        assertEquals("src/main/resources/security/config.properties", prop.getProperty("security.config.filepath"));
    }

    @Test
    public void test() {
        System.out.println(Arrays.toString(Security.getProviders()));
    }
}
