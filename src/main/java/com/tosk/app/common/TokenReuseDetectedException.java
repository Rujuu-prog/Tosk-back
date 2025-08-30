package com.tosk.app.common;

public class TokenReuseDetectedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public TokenReuseDetectedException(String message) {
    super(message);
  }
}
