package com.tosk.app.common;

public class TokenReuseDetectedException extends RuntimeException {
  public TokenReuseDetectedException(String message) {
    super(message);
  }
}
