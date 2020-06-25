package io.jenkins.plugins.credentials.secretsmanager.config;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.credentials.secretsmanager.util.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class PluginCasCConfigurationIT extends AbstractPluginConfigurationIT {
    @Rule
    public JenkinsRule r = new JenkinsConfiguredWithCodeRule();

    @Override
    protected PluginConfiguration getPluginConfiguration() {
        return (PluginConfiguration) r.jenkins.getDescriptor(PluginConfiguration.class);
    }

    @Override
    protected void setEndpointConfiguration(String serviceEndpoint, String signingRegion) {
        // no-op (configured by annotations)
    }

    @Override
    protected void setFilters(Filter... filters) {
        // no-op (configured by annotations)
    }

    @Override
    protected void setRoles(String role) {
        // no-op (configured by annotations)
    }

    @Override
    @Test
    @ConfiguredWithCode("/default.yml")
    public void shouldHaveDefaultConfiguration() {
        super.shouldHaveDefaultConfiguration();
    }

    @Override
    @Test
    @ConfiguredWithCode("/custom-endpoint-configuration.yml")
    public void shouldCustomiseEndpointConfiguration() {
        super.shouldCustomiseEndpointConfiguration();
    }

    @Override
    @Test
    @ConfiguredWithCode("/custom-filters.yml")
    public void shouldCustomiseFilters() {
        super.shouldCustomiseFilters();
    }

    @Test
    @ConfiguredWithCode("/multiple-filters.yml")
    public void shouldCustomiseMultipleFilters() {
        // Given
        setFilters(
                new Filter("tag-key", Lists.of(new Value("foo"))),
                new Filter("tag-value", Lists.of(new Value("bar"))));

        // When
        final PluginConfiguration config = getPluginConfiguration();

        // Then
        assertThat(config.getListSecrets().getFilters())
                .extracting("key", "values")
                .contains(
                        tuple("tag-key", Lists.of(new Value("foo"))),
                        tuple("tag-value", Lists.of(new Value("bar"))));
    }

    @Override
    @Test
    @ConfiguredWithCode("/custom-roles.yml")
    public void shouldCustomiseRoles() {
        super.shouldCustomiseRoles();
    }
}
