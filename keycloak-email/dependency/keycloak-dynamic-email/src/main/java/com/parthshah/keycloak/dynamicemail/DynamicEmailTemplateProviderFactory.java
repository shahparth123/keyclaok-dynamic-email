package com.parthshah.keycloak.dynamicemail;
import org.keycloak.Config;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.theme.FreeMarkerUtil;

import java.util.logging.Logger;

public class DynamicEmailTemplateProviderFactory extends FreeMarkerEmailTemplateProviderFactory {
    private static final Logger LOG = Logger.getLogger(DynamicEmailTemplateProviderFactory.class.getName());

    private FreeMarkerUtil freeMarker;
    private String defaultServerUrl;
    private String secretKey;
    private Boolean allowOverwriteServerUrl;
    @Override
    public DynamicEmailTemplateProvider create(KeycloakSession session) {
        LOG.info("dynamic init");
        LOG.info(session.toString());

        return new DynamicEmailTemplateProvider(session, this.freeMarker,defaultServerUrl,secretKey,allowOverwriteServerUrl);
    }

    @Override
    public void init(Config.Scope config) {
        freeMarker = new FreeMarkerUtil();
        defaultServerUrl= config.get("defaultServerUrl");
        secretKey= config.get("secretKey");
        allowOverwriteServerUrl= Boolean.valueOf(config.get("allowOverwriteServerUrl"));
    }

    public void postInit(KeycloakSessionFactory factory) {
    }
    public void close() {
        this.freeMarker = null;
    }

    @Override
    public String getId() {
        return "dynamic-email";
    }

}
