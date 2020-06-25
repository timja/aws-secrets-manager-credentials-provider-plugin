package io.jenkins.plugins.credentials.secretsmanager.supplier;

import com.amazonaws.SdkBaseException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.Filter;
import com.amazonaws.services.secretsmanager.model.SecretListEntry;
import com.amazonaws.services.secretsmanager.model.Tag;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import io.jenkins.plugins.credentials.secretsmanager.AssumeRoleDefaults;
import io.jenkins.plugins.credentials.secretsmanager.FiltersFactory;
import io.jenkins.plugins.credentials.secretsmanager.config.*;
import io.jenkins.plugins.credentials.secretsmanager.factory.CredentialsFactory;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CredentialsSupplier implements Supplier<Collection<StandardCredentials>> {

    private static final Logger LOG = Logger.getLogger(CredentialsSupplier.class.getName());

    private CredentialsSupplier() {

    }

    public static Supplier<Collection<StandardCredentials>> standard() {
        return new CredentialsSupplier();
    }

    @Override
    public Collection<StandardCredentials> get() {
        LOG.log(Level.FINE,"Retrieve secrets from AWS Secrets Manager");

        final PluginConfiguration config = PluginConfiguration.getInstance();

        final EndpointConfiguration ec = config.getEndpointConfiguration();
        final AwsClientBuilder.EndpointConfiguration endpointConfiguration = newEndpointConfiguration(ec);

        final List<io.jenkins.plugins.credentials.secretsmanager.config.Filter> filtersConfig = Optional.ofNullable(config.getListSecrets())
                .map(ListSecrets::getFilters)
                .orElse(Collections.emptyList());
        final Collection<Filter> filters = FiltersFactory.create(filtersConfig);

        final Roles roles = Optional.ofNullable(config.getBeta()).map(beta -> beta.getRoles()).orElse(null);
        final List<String> roleArns = newRoleArns(roles);

        final Supplier<Collection<StandardCredentials>> mainSupplier =
                new SingleAccountCredentialsSupplier(newClient(endpointConfiguration), SecretListEntry::getName, filters);

        final Collection<Supplier<Collection<StandardCredentials>>> otherSuppliers = roleArns.stream()
                .map(roleArn -> new SingleAccountCredentialsSupplier(newClient(roleArn, endpointConfiguration), SecretListEntry::getARN, filters))
                .collect(Collectors.toList());

        final ParallelSupplier<Collection<StandardCredentials>> allSuppliers = new ParallelSupplier<>(Lists.concat(mainSupplier, otherSuppliers));

        try {
            return allSuppliers.get()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(StandardCredentials::getId, Function.identity()))
                    .values();
        } catch (CompletionException | IllegalStateException e) {
            // Re-throw in a way that the provider knows how to catch
            throw new SdkBaseException(e.getCause());
        }
    }

    private static AWSSecretsManager newClient(AwsClientBuilder.EndpointConfiguration endpointConfiguration) {
        return AWSSecretsManagerClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }

    private static AWSSecretsManager newClient(String roleArn, AwsClientBuilder.EndpointConfiguration endpointConfiguration) {
        final AWSCredentialsProvider roleCredentials = new STSAssumeRoleSessionCredentialsProvider.Builder(roleArn, AssumeRoleDefaults.SESSION_NAME)
                .withRoleSessionDurationSeconds(AssumeRoleDefaults.SESSION_DURATION_SECONDS)
                .build();

        return AWSSecretsManagerClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(roleCredentials)
                .build();
    }

    private static List<String> newRoleArns(Roles roles) {
        if (roles != null && roles.getArns() != null) {
            return roles.getArns().stream().map(ARN::getValue).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static AwsClientBuilder.EndpointConfiguration newEndpointConfiguration(EndpointConfiguration ec) {
        if (ec == null || (ec.getServiceEndpoint() == null || ec.getSigningRegion() == null)) {
            return null;
        } else {
            return new AwsClientBuilder.EndpointConfiguration(ec.getServiceEndpoint(), ec.getSigningRegion());
        }
    }

    private static class SingleAccountCredentialsSupplier implements Supplier<Collection<StandardCredentials>> {

        private final AWSSecretsManager client;
        private final Function<SecretListEntry, String> nameSelector;
        private final Collection<Filter> filters;

        SingleAccountCredentialsSupplier(AWSSecretsManager client, Function<SecretListEntry, String> nameSelector, Collection<Filter> filters) {
            this.client = client;
            this.nameSelector = nameSelector;
            this.filters = filters;
        }

        @Override
        public Collection<StandardCredentials> get() {
            final Collection<SecretListEntry> secretList = new ListSecretsOperation(client, filters).get();

            return secretList.stream()
                    .flatMap(secretListEntry -> {
                        final String name = nameSelector.apply(secretListEntry);
                        final String description = Optional.ofNullable(secretListEntry.getDescription()).orElse("");
                        final Map<String, String> tags = Lists.toMap(secretListEntry.getTags(), Tag::getKey, Tag::getValue);
                        final Optional<StandardCredentials> cred = CredentialsFactory.create(name, description, tags, client);
                        return Optionals.stream(cred);
                    })
                    .collect(Collectors.toList());
        }
    }
}
