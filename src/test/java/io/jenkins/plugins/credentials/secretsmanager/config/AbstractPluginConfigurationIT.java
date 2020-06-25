package io.jenkins.plugins.credentials.secretsmanager.config;

import io.jenkins.plugins.credentials.secretsmanager.util.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.groups.Tuple.tuple;

public abstract class AbstractPluginConfigurationIT {

    protected abstract PluginConfiguration getPluginConfiguration();

    protected abstract void setEndpointConfiguration(String serviceEndpoint, String signingRegion);

    protected abstract void setFilters(Filter... filters);

    /*
     * Only 1 role is supported for now (because locating the Add button in HtmlUnit is difficult)
     */
    protected abstract void setRoles(String role);

    @Test
    public void shouldHaveDefaultConfiguration() {
        final PluginConfiguration config = getPluginConfiguration();

        assertSoftly(s -> {
            s.assertThat(config.getEndpointConfiguration()).as("Endpoint Configuration").isNull();
            s.assertThat(config.getBeta()).as("Beta Features").isNull();
            s.assertThat(config.getListSecrets()).as("ListSecrets").isNull();
        });
    }

    @Test
    public void shouldCustomiseEndpointConfiguration() {
        // Given
        setEndpointConfiguration("http://localhost:4584", "us-east-1");

        // When
        final PluginConfiguration config = getPluginConfiguration();

        // Then
        assertSoftly(s -> {
            s.assertThat(config.getEndpointConfiguration().getServiceEndpoint()).as("Service Endpoint").isEqualTo("http://localhost:4584");
            s.assertThat(config.getEndpointConfiguration().getSigningRegion()).as("Signing Region").isEqualTo("us-east-1");
        });
    }

    @Test
    public void shouldCustomiseFilters() {
        // Given
        setFilters(new Filter("name", Lists.of(new Value("foo"))));

        // When
        final PluginConfiguration config = getPluginConfiguration();

        // Then
        assertThat(config.getListSecrets().getFilters())
                .extracting("key", "values")
                .contains(tuple("name", Lists.of(new Value("foo"))));
    }

    @Test
    public void shouldCustomiseRoles() {
        // Given
        final String foo = "arn:aws:iam::111111111111:role/foo-role";
        setRoles(foo);

        // When
        final PluginConfiguration config = getPluginConfiguration();

        // Then
        assertThat(config.getBeta().getRoles().getArns())
                .extracting("value")
                .containsOnly(foo);
    }
}
