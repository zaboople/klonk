package org.tmotte.klonk.config.option;

public class SSHOptions {
  private String knownHostsFilename;
  private String privateKeyFilename;

  // GETS: //
  public String getKnownHostsFilename() {
    return knownHostsFilename;
  }  
  public String getPrivateKeyFilename() {
    return privateKeyFilename;
  }  
  
  // SETS: //
  public SSHOptions setKnownHostsFilename(String name) {
    this.knownHostsFilename=name;
    return this;
  }
  public SSHOptions setPrivateKeyFilename(String name) {
    this.privateKeyFilename=name;
    return this;
  }
}