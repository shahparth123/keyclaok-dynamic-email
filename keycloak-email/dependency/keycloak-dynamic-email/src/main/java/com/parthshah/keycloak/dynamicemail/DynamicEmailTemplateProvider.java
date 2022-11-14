package com.parthshah.keycloak.dynamicemail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
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

    private static final Logger LOG = Logger.getLogger(DynamicEmailTemplateProvider.class.getName());
    private String defaultServerUrl;
    private String secretKey;
    private Boolean allowOverwriteServerUrl;

    protected KeycloakSession session;
    protected AuthenticationSessionModel authenticationSession;
    protected FreeMarkerUtil freeMarker;
    protected RealmModel realm;
    protected UserModel user;
    protected final Map<String, Object> attributes = new HashMap();

    ObjectMapper objectMapper = new ObjectMapper();
    public DynamicEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker, String defaultServerUrl,String secretKey,Boolean allowOverwriteServerUrl) {
        super(session, freeMarker);
        this.session=session;
        this.defaultServerUrl=defaultServerUrl;
        this.secretKey=secretKey;
        this.allowOverwriteServerUrl=allowOverwriteServerUrl;
    }

    public EmailTemplateProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    public EmailTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    public EmailTemplateProvider setAttribute(String name, Object value) {
        this.attributes.put(name, value);
        return this;
    }

    public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
        return this;
    }

    protected String getRealmName() {
        return this.realm.getDisplayName() != null ? this.realm.getDisplayName() : ObjectUtil.capitalize(this.realm.getName());
    }


    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {
        LOG.info(this.session.toString());
        LOG.info(this.session.getContext().getRealm().toString());
        this.setRealm(this.session.getContext().getRealm());
        this.setUser(user);
        Map<String, Object> attributes = new HashMap(this.attributes);
        attributes.put("user", new ProfileBean(user));
        attributes.put("realmName", this.realm.getName());
        LOG.info("subject emailTestSubject");

        try {
            FreeMarkerEmailTemplateProvider.EmailTemplate email = this.processTestTemplate("emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
            LOG.info("Test tempalte processed");

            this.sendCustom(config, email.getSubject(), email.getTextBody(), email.getHtmlBody());
        }
        catch (Exception e){
            LOG.info("dynamic template test mail fail"+e.getMessage());
            throw new EmailException("dynamic sendSmtpTestEmail failed",e);
        }
    }

    protected void sendCustom(Map<String, String> config, String subject, String textBody, String htmlBody) throws EmailException {
        this.sendCustom((Map)config, (String)subject, textBody, (String)htmlBody, (String)null);
    }

    protected void sendCustom(Map<String, String> config, String subject, String textBody, String htmlBody, String address) throws EmailException {
        EmailSenderProvider emailSender = (EmailSenderProvider)this.session.getProvider(EmailSenderProvider.class);
        if (address == null) {
            LOG.info(this.user.getEmail());
            emailSender.send(config, this.user, subject, textBody, htmlBody);
        } else {
            emailSender.send(config, address, subject, textBody, htmlBody);
        }

    }
    protected void send(Map<String, String> config, String subject, String textBody, String htmlBody, String address) throws EmailException {
        EmailSenderProvider emailSender = (EmailSenderProvider)this.session.getProvider(EmailSenderProvider.class);
        if (address == null) {
            LOG.info(this.user.getEmail());
            emailSender.send(config, this.user, subject, textBody, htmlBody);
        } else {
            emailSender.send(config, address, subject, textBody, htmlBody);
        }

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
                LOG.info("subject "+subjectFormatKey);
                LOG.info("bodyTemplate "+ bodyTemplate);

                email = this.processDynamicTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
            }
            catch (Exception e){
                LOG.info("error "+e.getMessage());
                email = this.processTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
            }
            this.send(email.getSubject(), email.getTextBody(), email.getHtmlBody(),null);
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
        LOG.info("serverUrl"+defaultServerUrl);
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
        LOG.info("All properties fetched");

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
        LOG.info("All actions fetched");


        TemplateResponse templateBody=objectMapper.readValue(httpRequestClient(serverURL,requestAttributes),TemplateResponse.class);
        LOG.info("Object Mapper worked");

        /*
        * Following attributes can be sent to external server but we are not utilizing that due to security concerns
        * */
        requestAttributes.put("link",attributes.getOrDefault("link",""));
        requestAttributes.put("linkExpiration",attributes.getOrDefault("linkExpiration",""));
        requestAttributes.put("url",attributes.getOrDefault("url",null));
        LOG.info("templating starting");

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
            LOG.info("templating failed");

            return new FreeMarkerEmailTemplateProvider.EmailTemplate("", "", "htmlBody");
        }
    }



    protected FreeMarkerEmailTemplateProvider.EmailTemplate processTestTemplate(String subjectKey, List<Object> subjectAttributes, String template, Map<String, Object> attributes) throws EmailException {
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
            LOG.info("reached before process");
            String textBody = "Test Email";

            String htmlBody="Test Email";

            return new FreeMarkerEmailTemplateProvider.EmailTemplate(subject, textBody, htmlBody);
        } catch (Exception var16) {
            LOG.info("error "+var16.getMessage());
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
            LOG.info("Network request failed"+e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            LOG.info("Network request interrupted"+e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e){
            LOG.info("httpClient failed interrupted"+e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

}
