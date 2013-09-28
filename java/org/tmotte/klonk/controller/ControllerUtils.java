package org.tmotte.klonk.controller;
import java.io.File;

class ControllerUtils {
  static String getFullPath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}