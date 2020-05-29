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
                ApplicationProperties.from(getClass(), "/application.properties");
        assertThat(properties.isSuccess(), is(true));
        Properties p = properties.get();

        assertThat(p.getProperty("local.port"), equalTo("8000"));
        assertThat(p.getProperty("security.config.filepath"),
                   equalTo("/security/security-config.properties"));
    }

    @Test
    public void test_from_failure() {
        Try<Properties> properties = ApplicationProperties.from(getClass(), "dd");
        assertThat(properties.isFailure(), is(true));
    }
}
