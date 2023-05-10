package com.parthshah.keycloak.dynamicemail;
import org.keycloak.Config;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.EmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import java.util.logging.Logger;

public class DynamicEmailTemplateProviderFactory implements EmailTemplateProviderFactory {
    private static final Logger LOG = Logger.getLogger(DynamicEmailTemplateProviderFactory.class.getName());

    //private FreeMarkerUtil freeMarker;
    private String defaultServerUrl;
    private String secretKey;
    private Boolean allowOverwriteServerUrl;
    @Override
    public EmailTemplateProvider create(KeycloakSession session) {

        LOG.info("Dynamic Template provider initialization");
        return new DynamicEmailTemplateProvider(session, defaultServerUrl,secretKey,allowOverwriteServerUrl);
    }

    @Override
    public void init(Config.Scope config) {
        //freeMarker = new FreeMarkerUtil();
        defaultServerUrl= config.get("defaultServerUrl");
        secretKey= config.get("secretKey");
        allowOverwriteServerUrl= Boolean.valueOf(config.get("allowOverwriteServerUrl"));
        LOG.info("Configuring default server url:"+defaultServerUrl);
        if(defaultServerUrl==null){
            LOG.warning("defaultServerUrl is null");
        }
        if(secretKey==null){
            LOG.warning("secretKey is null");
        }
        if(allowOverwriteServerUrl==null){
            LOG.warning("allowOverwriteServerUrl is null");
        }
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "dynamic-email";
    }

}
