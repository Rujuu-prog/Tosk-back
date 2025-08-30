package com.tosk.app.common;

public final class ApiConstants {
  private ApiConstants() {}

  // Cookies (defaults; actual names can be overridden by properties)
  public static final String COOKIE_AT = "AT";
  public static final String COOKIE_RT = "RT";

  // Query Params
  public static final String PARAM_TOKEN = "token";
  public static final String PARAM_EMAIL = "email";

  // Error Codes
  public static final String ERR_VALIDATION = "VALIDATION_ERROR";
  public static final String ERR_UNAUTHORIZED = "AUTH_INVALID_CREDENTIALS";
  public static final String ERR_RATE_LIMIT = "AUTH_RATE_LIMIT";
  public static final String ERR_TOKEN_INVALID = "TOKEN_INVALID";
  public static final String ERR_TOKEN_MISSING = "TOKEN_MISSING";
  public static final String ERR_NOT_FOUND = "NOT_FOUND";
  public static final String ERR_TOKEN_REUSE_DETECTED = "TOKEN_REUSE_DETECTED";

  // Audit Events
  public static final String EVT_SIGNUP_SUCCESS = "AUTH_SIGNUP_SUCCESS";
  public static final String EVT_LOGIN_SUCCESS = "AUTH_LOGIN_SUCCESS";
  public static final String EVT_LOGIN_FAILED = "AUTH_LOGIN_FAILED";
  public static final String EVT_RT_REUSE = "AUTH_RT_REUSE_DETECTED";
  public static final String EVT_LOGOUT = "AUTH_LOGOUT";
  public static final String EVT_VERIFY_START = "AUTH_EMAIL_VERIFY_START";
  public static final String EVT_VERIFY_CONFIRMED = "AUTH_EMAIL_VERIFY_CONFIRMED";
  public static final String EVT_PW_RESET_START = "AUTH_PW_RESET_START";
  public static final String EVT_PW_RESET_CONFIRMED = "AUTH_PW_RESET_CONFIRMED";
  public static final String EVT_SESSIONS_REVOKE_ALL = "AUTH_SESSIONS_REVOKE_ALL";
}
