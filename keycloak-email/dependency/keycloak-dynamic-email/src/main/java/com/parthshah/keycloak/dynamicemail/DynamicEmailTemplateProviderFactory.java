package com.parthshah.keycloak.dynamicemail;
import org.keycloak.Config;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.theme.FreeMarkerUtil;

public class DynamicEmailTemplateProviderFactory extends FreeMarkerEmailTemplateProviderFactory {

    private FreeMarkerUtil freeMarker;
    private String defaultServerUrl;
    private String secretKey;
    private Boolean allowOverwriteServerUrl;
    @Override
    public DynamicEmailTemplateProvider create(KeycloakSession session) {
        return new DynamicEmailTemplateProvider(session, freeMarker,defaultServerUrl,secretKey,allowOverwriteServerUrl);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
        defaultServerUrl= config.get("defaultServerUrl");
        secretKey= config.get("secretKey");
        allowOverwriteServerUrl= Boolean.valueOf(config.get("allowOverwriteServerUrl"));
    }

    @Override
    public String getId() {
        return "dynamic-email";
    }

}
