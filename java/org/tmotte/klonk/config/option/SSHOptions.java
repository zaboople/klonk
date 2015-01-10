package org.tmotte.klonk.config.option;

public class SSHOptions {
  private String knownHostsFilename;
  private String privateKeysFilename;

  // GETS: //
  public String getKnownHostsFilename() {
    return knownHostsFilename;
  }  
  public String getPrivateKeysFilename() {
    return privateKeysFilename;
  }  
  
  // SETS: //
  public SSHOptions setKnownHostsFilename(String name) {
    this.knownHostsFilename=name;
    return this;
  }
  public SSHOptions setPrivateKeysFilename(String name) {
    this.privateKeysFilename=name;
    return this;
  }
  
  public String toString(){
    return "Known Hosts:"+knownHostsFilename
        +"\nPrivate keys:"+privateKeysFilename;
  }
}
