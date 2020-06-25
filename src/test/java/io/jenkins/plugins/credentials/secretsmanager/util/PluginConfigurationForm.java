package io.jenkins.plugins.credentials.secretsmanager.util;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import io.jenkins.plugins.credentials.secretsmanager.config.Filter;

import java.io.IOException;
import java.util.Optional;

public class PluginConfigurationForm {

    private final HtmlForm form;

    public PluginConfigurationForm(HtmlForm form) {
        this.form = form;
    }

    public void setFilter(Filter filter) {
        form.getInputByName("_.listSecrets").setChecked(true);

        // TODO support multiple filters
        clickRepeatableAddButton("Filters");

        form.getSelectByName("_.key").setSelectedAttribute(filter.getKey(), true);
        // TODO support multiple filter values
        // FIXME this is a hack that skips the 'other' _.value input in the form
        form.getInputsByName("_.value").get(1).setValueAttribute(filter.getValues().get(0).getValue());
    }

    public void clearRoles() {
        form.getInputByName("_.roles").setChecked(false);
    }

    public void setRole(String arn) {
        form.getInputByName("_.beta").setChecked(true);
        form.getInputByName("_.roles").setChecked(true);
        // TODO Use the 'Add' button to test multiple roles
        final HtmlInput input = form
                .getElementsByAttribute("div", "name", "arns").get(0)
                .getOneHtmlElementByAttribute("input", "name", "_.value");
        input.setValueAttribute(arn);
    }

    public void clearEndpointConfiguration() {
        form.getInputByName("_.endpointConfiguration").setChecked(false);
    }

    public void setEndpointConfiguration(String serviceEndpoint, String signingRegion) {
        form.getInputByName("_.endpointConfiguration").setChecked(true);
        form.getInputByName("_.serviceEndpoint").setValueAttribute(serviceEndpoint);
        form.getInputByName("_.signingRegion").setValueAttribute(signingRegion);
    }

    private Optional<String> getValidateSuccessMessage() {
        return form.getElementsByAttribute("div", "class", "ok")
                .stream()
                .map(DomNode::getTextContent)
                .filter(msg -> !msg.equalsIgnoreCase("Without a resource root URL, resources will be served from the main domain with Content-Security-Policy set."))
                .findFirst();
    }

    private String getValidateErrorMessage() {
        return form.getOneHtmlElementByAttribute("div", "class", "error").getTextContent();
    }

    private void clickRepeatableAddButton(String settingName) {
        form.getByXPath(String.format("//td[contains(text(), '%s')]/following-sibling::td[@class='setting-main']//span[contains(string(@class),'repeatable-add')]//button[contains(text(), 'Add')]", settingName))
                .stream()
                .findFirst()
                .ifPresent(button -> clickOrThrowException((HtmlButton) button));
    }

    public FormValidationResult clickValidateButton(String textContent) {
        form.getByXPath(String.format("//span[contains(string(@class),'validate-button')]//button[contains(text(), '%s')]", textContent))
                .stream()
                .findFirst()
                .ifPresent(button -> clickOrThrowException((HtmlButton) button));

        final Optional<String> successMessage = this.getValidateSuccessMessage();
        if (successMessage.isPresent()) {
            return FormValidationResult.success(successMessage.get());
        } else {
            final String failureMessage = this.getValidateErrorMessage();
            return FormValidationResult.error(failureMessage);
        }
    }

    private static void clickOrThrowException(HtmlButton button) {
        try {
            button.click();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
