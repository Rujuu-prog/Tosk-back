package com.tosk.app.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditLogger {
  private static final Logger log = LoggerFactory.getLogger("AUDIT");

  private AuditLogger() {}

  public static void info(String event, Map<String, Object> fields) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("event", event);
    if (fields != null) {
      m.putAll(fields);
    }
    log.info("{}", m);
  }

  public static void warn(String event, Map<String, Object> fields) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("event", event);
    if (fields != null) {
      m.putAll(fields);
    }
    log.warn("{}", m);
  }

  public static String hash(String input) {
    try {
      MessageDigest d = MessageDigest.getInstance("SHA-256");
      byte[] h = d.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < h.length; i++) {
        sb.append(String.format("%02x", h[i]));
      }
      return sb.substring(0, 16);
    } catch (Exception e) {
      return "na";
    }
  }
}
