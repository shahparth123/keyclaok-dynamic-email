package com.parthshah.keycloak.dynamicemail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.EventBean;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.freemarker.model.UrlBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.FreeMarkerUtil;
import org.keycloak.theme.Theme;
import org.keycloak.theme.beans.LinkExpirationFormatterMethod;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DynamicEmailTemplateProvider implements EmailTemplateProvider {

    private static final Logger LOG = Logger.getLogger(DynamicEmailTemplateProvider.class.getName());
    protected final Map<String, Object> attributes = new HashMap<>();
    protected String defaultServerUrl;
    protected String secretKey;
    protected Boolean allowOverwriteServerUrl;
    protected KeycloakSession session;
    protected AuthenticationSessionModel authenticationSession;
    protected RealmModel realm;
    protected UserModel user;
    protected FreeMarkerUtil freeMarker;
    ObjectMapper objectMapper = new ObjectMapper();

    public DynamicEmailTemplateProvider(KeycloakSession session, FreeMarkerUtil freeMarker, String defaultServerUrl, String secretKey, Boolean allowOverwriteServerUrl) {
        this.session = session;
        this.freeMarker = freeMarker;
        this.defaultServerUrl = defaultServerUrl;
        this.secretKey = secretKey;
        this.allowOverwriteServerUrl = allowOverwriteServerUrl;
    }

    @Override
    public EmailTemplateProvider setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public EmailTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public EmailTemplateProvider setAttribute(String name, Object value) {
        this.attributes.put(name, value);
        return this;
    }

    @Override
    public EmailTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        this.authenticationSession = authenticationSession;
        return this;
    }

    protected String getRealmName() {
        return this.realm.getDisplayName() != null ? this.realm.getDisplayName() : ObjectUtil.capitalize(this.realm.getName());
    }


    @Override
    public void send(String subjectFormatKey, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        this.send(subjectFormatKey, Collections.emptyList(), bodyTemplate, bodyAttributes);
    }

    @Override
    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes) throws EmailException {
        try {
            DynamicEmailTemplate email;
            try {
                email = this.processDynamicTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
                this.send(email.getSubject(), email.getTextBody(), email.getHtmlBody());
            } catch (Exception e) {
                LOG.warning("error " + e.getMessage());
                throw new EmailException("Failed to template email", e);

            }
        } catch (EmailException var6) {
            throw var6;
        } catch (Exception var7) {
            throw new EmailException("Failed to template email", var7);
        }
    }

    public void send(String subjectFormatKey, List<Object> subjectAttributes, String bodyTemplate, Map<String, Object> bodyAttributes, String address) throws EmailException {
        try {
            DynamicEmailTemplate email;
            try {
                email = this.processDynamicTemplate(subjectFormatKey, subjectAttributes, bodyTemplate, bodyAttributes);
                this.send(email.getSubject(), email.getTextBody(), email.getHtmlBody(), address);
            } catch (Exception e) {
                LOG.warning("error " + e.getMessage());
                throw new EmailException("Failed to template email", e);
            }
        } catch (EmailException var6) {
            throw var6;
        } catch (Exception var7) {
            throw new EmailException("Failed to template email", var7);
        }
    }

    protected void send(String subject, String textBody, String htmlBody) throws EmailException {
        send(realm.getSmtpConfig(), subject, textBody, htmlBody, null);
    }


    protected void send(String subject, String textBody, String htmlBody, String address) throws EmailException {
        send(realm.getSmtpConfig(), subject, textBody, htmlBody, address);
    }

    protected void send(Map<String, String> config, String subject, String textBody, String htmlBody, String address) throws EmailException {
        EmailSenderProvider emailSender = (EmailSenderProvider) this.session.getProvider(EmailSenderProvider.class);
        if (address == null) {
            //LOG.info(this.user.getEmail());
            emailSender.send(config, this.user.getEmail(), subject, textBody, htmlBody);
        } else {
            emailSender.send(config, address, subject, textBody, htmlBody);
        }
    }

    protected void send(Map<String, String> config, String subject, String textBody, String htmlBody) throws EmailException {
        EmailSenderProvider emailSender = session.getProvider(EmailSenderProvider.class);
        emailSender.send(config, user, subject, textBody, htmlBody);
    }


    @Override
    public void sendEvent(Event event) throws EmailException {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user));
        attributes.put("event", new EventBean(event));
        send(toCamelCase(event.getType()) + "Subject", "event-" + event.getType().toString().toLowerCase() + ".ftl", attributes);
    }

    @Override
    public void sendPasswordReset(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(this.user));
        this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        this.send("passwordResetSubject", "password-reset.ftl", attributes);
    }


    @Override
    public void sendSmtpTestEmail(Map<String, String> config, UserModel user) throws EmailException {
        LOG.info("Sending test email for " + this.session.getContext().getRealm().getDisplayName());
        this.setRealm(this.session.getContext().getRealm());
        this.setUser(user);
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        attributes.put("realmName", this.realm.getName());
        LOG.info("Payload set in test email");

        try {
            DynamicEmailTemplate email = this.processDynamicTemplate("emailTestSubject", Collections.emptyList(), "email-test.ftl", attributes);
            LOG.info("Test template processed");
            this.send(config, email.getSubject(), email.getTextBody(), email.getHtmlBody());
            LOG.info("Test email sent");
        } catch (Exception e) {
            LOG.info("Dynamic template test mail fail " + e.getMessage());
            throw new EmailException("Dynamic sendSmtpTestEmail failed", e);
        }
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(this.user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get("identityProviderBrokerCtx");
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        String idpDisplayName = brokerContext.getIdpConfig().getDisplayName();
        idpAlias = ObjectUtil.capitalize(idpAlias);
        if (idpDisplayName != null) {
            idpAlias = ObjectUtil.capitalize(idpDisplayName);
        }
        attributes.put("identityProviderContext", brokerContext);
        attributes.put("identityProviderAlias", idpAlias);
        List<Object> subjectAttrs = Arrays.asList(idpAlias);
        this.send("identityProviderLinkSubject", (List) subjectAttrs, "identity-provider-link.ftl", (Map) attributes);
    }

    @Override
    public void sendExecuteActions(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", getRealmName());
        this.send("executeActionsSubject", "executeActions.ftl", attributes);
    }

    @Override
    public void sendVerifyEmail(String link, long expirationInMinutes) throws EmailException {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", new ProfileBean(user));
        this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
        attributes.put("realmName", this.getRealmName());
        this.send("emailVerificationSubject", "email-verification.ftl", (Map) attributes);
    }

    @Override
    public void sendEmailUpdateConfirmation(String link, long expirationInMinutes, String newEmail) throws EmailException {
        if (newEmail == null) {
            throw new IllegalArgumentException("The new email is mandatory");
        } else {
            Map<String, Object> attributes = new HashMap<>(this.attributes);
            attributes.put("user", new ProfileBean(this.user));
            attributes.put("newEmail", newEmail);
            this.addLinkInfoIntoAttributes(link, expirationInMinutes, attributes);
            attributes.put("realmName", this.getRealmName());
            this.send("emailUpdateConfirmationSubject", (List) Collections.emptyList(), "email-update-confirmation.ftl", (Map) attributes, newEmail);
        }
    }


    protected DynamicEmailTemplate processDynamicTemplate(String subjectKey, List<Object> subjectAttributes, String templateName, Map<String, Object> attributes) throws EmailException, JsonProcessingException {

        HashMap<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put("subjectFormatKey", subjectKey);
        requestAttributes.put("subjectAttributes", subjectAttributes);
        requestAttributes.put("templateName", templateName);
        requestAttributes.put("realmName", attributes.getOrDefault("realmName", ""));
        requestAttributes.put("identityProviderContext", attributes.getOrDefault("realmName", "").toString());
        requestAttributes.put("identityProviderAlias", attributes.getOrDefault("identityProviderAlias", "").toString());
        ProfileBean userProfile = (ProfileBean) attributes.getOrDefault("user", null);
        String serverURL = defaultServerUrl;
        if (!userProfile.equals(null)) {
            if (userProfile.getFirstName() != null) {
                requestAttributes.put("user.firstName", userProfile.getFirstName());
            } else {
                requestAttributes.put("user.firstName", "Guest");
            }
            if (userProfile.getFirstName() != null) {
                requestAttributes.put("user.lastName", userProfile.getLastName());
            } else {
                requestAttributes.put("user.lastName", "User");
            }
            if (userProfile.getEmail() != null) {
                requestAttributes.put("user.email", userProfile.getEmail());
            } else {
                requestAttributes.put("user.email", userProfile.getEmail());
            }
            requestAttributes.put("user.username", userProfile.getUsername());
            requestAttributes.put("user.attributes", userProfile.getAttributes());
            if (allowOverwriteServerUrl.equals(Boolean.TRUE)) {
                if (userProfile.getAttributes().containsKey("serverURL") && !userProfile.getAttributes().get("serverURL").equals("") && userProfile.getAttributes().get("serverURL") != null) {
                    serverURL = userProfile.getAttributes().get("serverURL");
                }
            }
        }
        //LOG.info("serverUrl" + defaultServerUrl);
        //LOG.info("All properties fetched");

        if (attributes.containsKey("event")) {
            EventBean eventBean = (EventBean) attributes.getOrDefault("event", null);
            if (eventBean != null) {
                requestAttributes.put("event.date", eventBean.getDate());
                requestAttributes.put("event.event", eventBean.getEvent());
                requestAttributes.put("event.client", eventBean.getClient());
                requestAttributes.put("event.ipAddress", eventBean.getIpAddress());
            }
        }


        List<String> requiredActions = (LinkedList<String>) attributes.getOrDefault("requiredActions", new LinkedList<>());
        HashMap<String, String> requiredActionsMap = new HashMap<>();
        int itemCount = 0;
        for (String item : requiredActions) {
            //LOG.log(Level.INFO, item);
            requiredActionsMap.put(String.valueOf(itemCount), item);
        }

        requestAttributes.put("requiredActions", requiredActionsMap);
        //LOG.info("All actions fetched");

        String response = httpRequestClient(serverURL, requestAttributes);
        //LOG.info(response);

        TemplateResponse templateBody = objectMapper.readValue(response, TemplateResponse.class);
        //LOG.info("Object Mapper worked");

        /*
         * Following attributes can be sent to external server, but we are not utilizing that due to security concerns
         * */
        requestAttributes.put("link", attributes.getOrDefault("link", ""));
        requestAttributes.put("linkExpiration", attributes.getOrDefault("linkExpiration", ""));
        requestAttributes.put("url", attributes.getOrDefault("url", ""));
        //LOG.info("templating starting");

        try {
//            flattenAttributeMap.entrySet().stream()
//                    .forEach(e -> LOG.log(Level.INFO,e.getKey()));

            String subject = templateBody.subject;
            String htmlBody = templateBody.htmlTemplate;
            String textBody = templateBody.textTemplate;

            if (subject == null) {
                subject = "";
            }
            if (htmlBody == null) {
                htmlBody = "";
            }
            if (textBody == null) {
                textBody = "";
            }

           // LOG.info("flattening request attributes");
//            LOG.info(objectMapper
//                    .writerWithDefaultPrettyPrinter()
//                    .writeValueAsString(requestAttributes));
            Map<String, Object> flattenAttributeMap = requestAttributes.entrySet().stream()
                    .flatMap(FlattenMap::flatten)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            for (Map.Entry<String, Object> item : flattenAttributeMap.entrySet()) {
                htmlBody = htmlBody.replace("${" + item.getKey() + "}", item.getValue().toString());
                textBody = textBody.replace("${" + item.getKey() + "}", item.getValue().toString());
                subject = subject.replace("${" + item.getKey() + "}", item.getValue().toString());
            }


            return new DynamicEmailTemplate(subject, textBody, htmlBody);
        } catch (Exception e) {
            LOG.warning("templating failed");
            return new DynamicEmailTemplate("testSubject", "testBody", "htmlBody");
        }
    }


    public String httpRequestClient(String requestURL, Map<String, Object> data) throws JsonProcessingException {
        var client = HttpClient.newHttpClient();
        String requestBody = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data);

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(requestURL))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (secretKey != null && !secretKey.equals("")) {
            request.header("X-API-Key", secretKey);
        }
        HttpRequest requestPayload = request.POST(HttpRequest.BodyPublishers.ofString(requestBody)).
                build();
        try {
            var response = client.send(requestPayload, HttpResponse.BodyHandlers.ofString());
            return response.body().toString();
        } catch (IOException e) {
            LOG.warning("Network request failed" + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            LOG.warning("Network request interrupted" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LOG.warning("httpClient failed interrupted" + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }


    protected String toCamelCase(EventType event) {
        StringBuilder sb = new StringBuilder("event");
        for (String s : event.name().toLowerCase().split("_")) {
            sb.append(ObjectUtil.capitalize(s));
        }
        return sb.toString();
    }

    /**
     * Add link info into template attributes.
     *
     * @param link                to add
     * @param expirationInMinutes to add
     * @param attributes          to add link info into
     */
    protected void addLinkInfoIntoAttributes(String link, long expirationInMinutes, Map<String, Object> attributes) throws EmailException {
        attributes.put("link", link);
        attributes.put("linkExpiration", expirationInMinutes);
        KeycloakUriInfo uriInfo = session.getContext().getUri();
        URI baseUri = uriInfo.getBaseUri();
        try {
            Locale locale = session.getContext().resolveLocale(user);
            attributes.put("linkExpirationFormatter", new LinkExpirationFormatterMethod(getTheme().getMessages(locale), locale));
            attributes.put("url", new UrlBean(realm, getTheme(), baseUri, null));
        } catch (IOException e) {
            throw new EmailException("Failed to template email", e);
        }
    }

    protected Theme getTheme() throws IOException {
        return session.theme().getTheme(Theme.Type.EMAIL);
    }

    @Override
    public void close() {

    }


    protected class DynamicEmailTemplate {
        private String subject;
        private String textBody;
        private String htmlBody;

        public DynamicEmailTemplate(String subject, String textBody, String htmlBody) {
            this.subject = subject;
            this.textBody = textBody;
            this.htmlBody = htmlBody;
        }

        public String getSubject() {
            return this.subject;
        }

        public String getTextBody() {
            return this.textBody;
        }

        public String getHtmlBody() {
            return this.htmlBody;
        }
    }
}


