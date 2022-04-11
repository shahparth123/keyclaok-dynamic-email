package com.parthshah.keycloak.dynamicemail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.theme.FreeMarkerException;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.MessageFormatterMethod;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DynamicEmailTemplateProvider extends FreeMarkerEmailTemplateProvider {

    private static final Logger LOG = Logger.getLogger(FreeMarkerEmailTemplateProvider.class.getName());
    private String defaultServerUrl;
    private String secretKey;
    private Boolean allowOverwriteServerUrl;
    private KeycloakSession session;

    ObjectMapper objectMapper = new ObjectMapper();
    public DynamicEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker, String defaultServerUrl,String secretKey,Boolean allowOverwriteServerUrl) {
        super(session, freeMarker);
        this.session=session;
        this.defaultServerUrl=defaultServerUrl;
        this.secretKey=secretKey;
        this.allowOverwriteServerUrl=allowOverwriteServerUrl;
    }

    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {
        this.setRealm(this.session.getContext().getRealm());
        this.setUser(user);
        Map<String, Object> attributes = new HashMap(this.attributes);
        attributes.put("user", new ProfileBean(user));
        attributes.put("realmName", this.realm.getName());
        FreeMarkerEmailTemplateProvider.EmailTemplate email = this.processTemplate("emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
        this.send(config, email.getSubject(), email.getTextBody(), email.getHtmlBody());
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap(this.attributes);
        attributes.put("user", new ProfileBean(this.user));
        this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext)this.attributes.get("identityProviderBrokerCtx");
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        String idpDisplayName = brokerContext.getIdpConfig().getDisplayName();
        idpAlias = ObjectUtil.capitalize(idpAlias);
        if (idpDisplayName != null) {
            idpAlias = ObjectUtil.capitalize(idpDisplayName);
        }

        attributes.put("identityProviderContext", brokerContext);
        attributes.put("identityProviderAlias", idpAlias);
        List<Object> subjectAttrs = Arrays.asList(idpAlias);
        this.send((String)"identityProviderLinkSubject", (List)subjectAttrs, "identity-provider-link.ftl", (Map)attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap(this.attributes);
        attributes.put("user", new ProfileBean(this.user));
        this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        this.send("passwordResetSubject", "password-reset.ftl", (Map)attributes);
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap(this.attributes);
        attributes.put("user", new ProfileBean(this.user));
        this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        this.send("emailVerificationSubject", "email-verification.ftl", (Map)attributes);
    }

    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        this.send(subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes);
    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap(this.attributes);
        attributes.put("user", new ProfileBean(this.user));
        this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        this.send("executeActionsSubject", "executeActions.ftl", (Map)attributes);
    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        try {
            FreeMarkerEmailTemplateProvider.EmailTemplate email;
            try
            {
                email = this.processDynamicTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
            }
            catch (Exception e){
                email = this.processTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
            }
            this.send(email.getSubject(), email.getTextBody(), email.getHtmlBody());
        } catch (EmailException var6) {
            throw var6;
        } catch (Exception var7) {
            throw new EmailException("Failed to template email", var7);
        }
    }


    protected FreeMarkerEmailTemplateProvider.EmailTemplate processDynamicTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException, JsonProcessingException {

        HashMap<String,Object> requestAttributes=new HashMap<>();
//        for(Map.Entry<String,Object> attribute: attributes.entrySet())
//        {
//            LOG.log(Level.INFO,attribute.getKey());
//        }
        requestAttributes.put("subjectFormatKey",subjectKey);
        requestAttributes.put("realmName",attributes.getOrDefault("realmName",""));
        requestAttributes.put("identityProviderContext",attributes.getOrDefault("realmName","").toString());
        requestAttributes.put("identityProviderAlias",attributes.getOrDefault("identityProviderAlias","").toString());
        requestAttributes.put("realmName",attributes.getOrDefault("realmName",""));
        ProfileBean userProfile = (ProfileBean)attributes.getOrDefault("user",null);
        String serverURL=defaultServerUrl;

        if(!userProfile.equals(null))
        {
            if(userProfile.getFirstName()!=null)
            {
                requestAttributes.put("user.firstName",userProfile.getFirstName());
            }
            else{
                requestAttributes.put("user.firstName","Guest");
            }
            if(userProfile.getFirstName()!=null) {
                requestAttributes.put("user.lastName", userProfile.getLastName());
            }
            else{
                requestAttributes.put("user.lastName","User");
            }
            if(userProfile.getEmail()!=null){
                requestAttributes.put("user.email",userProfile.getEmail());
            }
            else{
                requestAttributes.put("user.email",userProfile.getEmail());
            }
            requestAttributes.put("user.username",userProfile.getUsername());
            requestAttributes.put("user.attributes",userProfile.getAttributes());
            if(allowOverwriteServerUrl.equals(Boolean.TRUE) ) {
            if(userProfile.getAttributes().containsKey("serverURL") && !userProfile.getAttributes().get("serverURL").equals("") && userProfile.getAttributes().get("serverURL")!=null){
                serverURL=userProfile.getAttributes().get("serverURL");
            }
            }
        }

        if(attributes.containsKey("event"))
        {
            EventBean eventBean = (EventBean)attributes.getOrDefault("event",null);
            if(eventBean!=null)
            {
                requestAttributes.put("event.date",eventBean.getDate());
                requestAttributes.put("event.event",eventBean.getEvent());
                requestAttributes.put("event.client",eventBean.getClient());
                requestAttributes.put("event.ipAddress",eventBean.getIpAddress());
            }
        }


        List<String> requiredActions = (LinkedList<String>)attributes.getOrDefault("requiredActions",new LinkedList<>());
        HashMap<String,String> requiredActionsMap=new HashMap<>();
        int itemCount=0;
        for(String item : requiredActions){
            LOG.log(Level.INFO,item);
            requiredActionsMap.put(String.valueOf(itemCount),item);
        }

        requestAttributes.put("requiredActions",requiredActionsMap);


        TemplateResponse templateBody=objectMapper.readValue(httpRequestClient(serverURL,requestAttributes),TemplateResponse.class);

        /*
        * Following attributes can be sent to external server but we are not utilizing that due to security concerns
        * */
        requestAttributes.put("link",attributes.getOrDefault("link",""));
        requestAttributes.put("linkExpiration",attributes.getOrDefault("linkExpiration",""));
        requestAttributes.put("url",attributes.getOrDefault("url",null));

        try {


            Map<String, Object> flattenAttributeMap = requestAttributes.entrySet().stream()
                    .flatMap(FlattenMap::flatten)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            flattenAttributeMap.entrySet().stream()
                    .forEach(e -> LOG.log(Level.INFO,e.getKey()));

            String subject=templateBody.subject;
            String htmlBody=templateBody.htmlTemplate;
            String textBody=templateBody.textTemplate;

            for(Map.Entry<String,Object> item :flattenAttributeMap.entrySet() )
            {
                htmlBody=htmlBody.replace("${"+item.getKey()+"}",item.getValue().toString());
                textBody=textBody.replace("${"+item.getKey()+"}",item.getValue().toString());
                subject=subject.replace("${"+item.getKey()+"}",item.getValue().toString());
            };



            return new FreeMarkerEmailTemplateProvider.EmailTemplate(subject, textBody, htmlBody);
        }
        catch (Exception e){
            return new FreeMarkerEmailTemplateProvider.EmailTemplate("", "", "htmlBody");
        }
    }


        @Override
    protected FreeMarkerEmailTemplateProvider.EmailTemplate processTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
        try {
            Theme theme = this.getTheme();
            Locale locale = this.session.getContext().resolveLocale(this.user);
            attributes.put("locale", locale);
            Properties rb = new Properties();
            rb.putAll(theme.getMessages(locale));
            rb.putAll(this.realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag()));
            attributes.put("msg", new MessageFormatterMethod(locale, rb));
            attributes.put("properties", theme.getProperties());
            String subject = (new MessageFormat(rb.getProperty(subjectKey, subjectKey), locale)).format(subjectAttributes.toArray());
            String textTemplate = String.format("text/%s", template);

            String textBody;
            try {
                textBody = this.freeMarker.processTemplate(attributes, textTemplate, theme);
            } catch (FreeMarkerException var15) {
                throw new EmailException("Failed to template plain text email.", var15);
            }

            String htmlTemplate = String.format("html/%s", template);

            String htmlBody;
            try {
                htmlBody = this.freeMarker.processTemplate(attributes, htmlTemplate, theme);
            } catch (FreeMarkerException var14) {
                throw new EmailException("Failed to template html email.", var14);
            }

            return new FreeMarkerEmailTemplateProvider.EmailTemplate(subject, textBody, htmlBody);
        } catch (Exception var16) {
            throw new EmailException("Failed to template email", var16);
        }
    }

    public String httpRequestClient(String requestURL,Map<String,Object> data) throws JsonProcessingException {
        var client = HttpClient.newHttpClient();
        String requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data);
        var request = HttpRequest.newBuilder(URI.create(requestURL))
                .header("Content-Type", "application/json")
                .header("X-API-Key",secretKey)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)).
                build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

}
