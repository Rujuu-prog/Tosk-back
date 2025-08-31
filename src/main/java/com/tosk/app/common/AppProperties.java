package com.tosk.app.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private String frontendBaseUrl = "https://localhost:3000";
  private Mail mail = new Mail();

  public String getFrontendBaseUrl() {
    return frontendBaseUrl;
  }

  public void setFrontendBaseUrl(String frontendBaseUrl) {
    this.frontendBaseUrl = frontendBaseUrl;
  }

  public Mail getMail() {
    return mail;
  }

  public static class Mail {
    private String from = "no-reply@tosk.local";
    private String subjectPrefix = "[TOSK] ";
    private String provider = "logging"; // logging|smtp|resend
    private String locale = "ja";

    public String getFrom() {
      return from;
    }

    public void setFrom(String from) {
      this.from = from;
    }

    public String getSubjectPrefix() {
      return subjectPrefix;
    }

    public void setSubjectPrefix(String subjectPrefix) {
      this.subjectPrefix = subjectPrefix;
    }

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public String getLocale() {
      return locale;
    }

    public void setLocale(String locale) {
      this.locale = locale;
    }
  }
}
