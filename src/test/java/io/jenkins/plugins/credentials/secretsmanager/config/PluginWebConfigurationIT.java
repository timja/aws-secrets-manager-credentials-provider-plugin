package io.jenkins.plugins.credentials.secretsmanager.config;

import io.jenkins.plugins.credentials.secretsmanager.util.JenkinsConfiguredWithWebRule;
import io.jenkins.plugins.credentials.secretsmanager.util.PluginConfigurationForm;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class PluginWebConfigurationIT extends AbstractPluginConfigurationIT {

    @Rule
    public final JenkinsConfiguredWithWebRule r = new JenkinsConfiguredWithWebRule();

    @Override
    protected PluginConfiguration getPluginConfiguration() {
        return (PluginConfiguration) r.jenkins.getDescriptor(PluginConfiguration.class);
    }

    @Override
    protected void setEndpointConfiguration(String serviceEndpoint, String signingRegion) {
        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);

            form.setEndpointConfiguration(serviceEndpoint, signingRegion);
        });
    }

    @Override
    protected void setFilters(Filter... filters) {
        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);

            form.setFilter(filters[0]);
        });
    }

    @Override
    protected void setRoles(String role) {
        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);

            form.setRole(role);
        });
    }

    @Test
    public void shouldCustomiseAndResetEndpointConfiguration() {
        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);
            form.setEndpointConfiguration("http://localhost:4584", "us-east-1");
        });

        final PluginConfiguration configBefore = getPluginConfiguration();

        assertThat(configBefore.getEndpointConfiguration()).isNotNull();

        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);
            form.clearEndpointConfiguration();
        });

        final PluginConfiguration configAfter = getPluginConfiguration();

        assertThat(configAfter.getEndpointConfiguration()).isNull();
    }

    @Test
    public void shouldCustomiseAndResetRoles() {
        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);
            form.setRole("arn:aws:iam::123456789012:role/marketingadminrole");
        });

        final PluginConfiguration configBefore = getPluginConfiguration();

        assertThat(configBefore.getBeta().getRoles()).isNotNull();

        r.configure(f -> {
            final PluginConfigurationForm form = new PluginConfigurationForm(f);
            form.clearRoles();
        });

        final PluginConfiguration configAfter = getPluginConfiguration();

        assertThat(configAfter.getBeta().getRoles()).isNull();
    }
}
