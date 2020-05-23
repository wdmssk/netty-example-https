package io.netty.https.util;


import io.vavr.control.Try;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


public class ApplicationPropertiesTest {
    @Test
    public void test_from() {
        Try<Properties> properties =
                ApplicationProperties.from("src/test/resources/security/security-application.properties");
        assertThat(properties.isSuccess(), is(true));
        Properties p = properties.get();

        assertThat(p.getProperty("local.port"), equalTo("8443"));
        assertThat(p.getProperty("security.config.filepath"),
                   equalTo("src/main/resources/security/security-config.properties"));
    }

    @Test
    public void test_from_failure() {
        Try<Properties> properties = ApplicationProperties.from("dd");
        assertThat(properties.isFailure(), is(true));
    }
}
