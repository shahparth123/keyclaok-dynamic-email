package com.parthshah.keycloak.dynamicemail;

import java.util.HashMap;
import java.util.Objects;

public class TemplateResponse {

    public String subject;
    public String htmlTemplate;
    public String textTemplate;
    public HashMap<String, String> extra;

    public TemplateResponse(String subject, String htmlTemplate, String textTemplate, HashMap<String, String> extra) {
        this.subject = subject;
        this.htmlTemplate = htmlTemplate;
        this.textTemplate = textTemplate;
        this.extra = extra;
    }

    public TemplateResponse() {
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public String getTextTemplate() {
        return textTemplate;
    }

    public void setTextTemplate(String textTemplate) {
        this.textTemplate = textTemplate;
    }

    public HashMap<String, String> getExtra() {
        return extra;
    }

    public void setExtra(HashMap<String, String> extra) {
        this.extra = extra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateResponse that = (TemplateResponse) o;
        return Objects.equals(subject, that.subject) && Objects.equals(htmlTemplate, that.htmlTemplate) && Objects.equals(textTemplate, that.textTemplate) && Objects.equals(extra, that.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, htmlTemplate, textTemplate, extra);
    }
}
