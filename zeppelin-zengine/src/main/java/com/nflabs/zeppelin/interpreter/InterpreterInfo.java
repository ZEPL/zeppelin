package com.nflabs.zeppelin.interpreter;

import java.util.Collection;

/**
 * Interpreter Info
 *
 */
public interface InterpreterInfo {
  /**
   * Get information by name
   */
  public String get(String name);

  /**
   * Get list of available information
   */
  public Collection<String> list();
}
