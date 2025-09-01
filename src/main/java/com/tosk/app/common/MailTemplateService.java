package com.tosk.app.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class MailTemplateService {
  public String render(String templatePath, Map<String, String> vars) {
    try {
      ClassPathResource res = new ClassPathResource(templatePath);
      String content = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (vars != null) {
        for (var e : vars.entrySet()) {
          content = content.replace("{{" + e.getKey() + "}}", e.getValue());
        }
      }
      return content;
    } catch (IOException e) {
      throw new IllegalStateException("メールテンプレート読み込み失敗: " + templatePath, e);
    }
  }

  public String renderLocalized(
      String baseName, String locale, String ext, Map<String, String> vars) {
    // baseName like "mail/verification", ext "txt" or "html"
    String loc = (locale == null || locale.isBlank()) ? "" : (locale + "/");
    String[] candidates =
        new String[] {
          "templates/" + baseName + "." + ext,
          "templates/" + baseName + "." + ext, // fallback duplicate for clarity
          "templates/"
              + baseName.substring(0, baseName.lastIndexOf('/') + 1)
              + loc
              + baseName.substring(baseName.lastIndexOf('/') + 1)
              + "."
              + ext
        };
    for (String path : candidates) {
      try {
        ClassPathResource res = new ClassPathResource(path);
        if (res.exists()) {
          String content =
              new String(
                  res.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
          if (vars != null) {
            for (var e : vars.entrySet()) {
              content = content.replace("{{" + e.getKey() + "}}", e.getValue());
            }
          }
          return content;
        }
      } catch (Exception ignore) {
      }
    }
    // Fallback to non-localized base
    return render("templates/" + baseName + "." + ext, vars);
  }
}
