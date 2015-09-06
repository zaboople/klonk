package org.tmotte.klonk.config.option;

public class SSHOptions {

  private String knownHostsFilename;
  private String privateKeysFilename;
  private String openSSHConfigFilename;

  /** These are the default file permissions */
  public boolean 
    dur=true,duw=true,dux=false,
    dgr=true,dgw=true,dgx=false,
    dor=false,dow=false,dox=false;

  ///////////
  // GETS: //
  ///////////

  public String getKnownHostsFilename() {
    return knownHostsFilename;
  }
  public String getPrivateKeysFilename() {
    return privateKeysFilename;
  }
  public String getOpenSSHConfigFilename() {
    return openSSHConfigFilename;
  }
  public String getDefaultFilePermissions() {
    return Integer.toString(
      getDigit(dox, 0)+
      getDigit(dow, 1)+
      getDigit(dor, 2)+
      getDigit(dgx, 3)+
      getDigit(dgw, 4)+
      getDigit(dgr, 5)+
      getDigit(dux, 6)+
      getDigit(duw, 7)+
      getDigit(dur, 8)
      ,
      8
    );
  }
  public String getDefaultDirPermissions() {
    return Integer.toString(
      getDigit(dor, 0)+
      getDigit(dow, 1)+
      getDigit(dor, 2)+
      getDigit(dgr, 3)+
      getDigit(dgw, 4)+
      getDigit(dgr, 5)+
      getDigit(dur, 6)+
      getDigit(duw, 7)+
      getDigit(dur, 8)
      ,
      8
    );
  }
  public String getRWXUser() {
    return 
      getPerm("r", dur)+
      getPerm("w", duw)+
      getPerm("x", dux);
  }
  public String getRWXGroup() {
    return 
      getPerm("r", dgr)+
      getPerm("w", dgw)+
      getPerm("x", dgx);
  }
  public String getRWXOther() {
    return 
      getPerm("r", dor)+
      getPerm("w", dow)+
      getPerm("x", dox);
  }
  
  ///////////
  // SETS: //
  ///////////

  public SSHOptions setKnownHostsFilename(String name) {
    this.knownHostsFilename=name;
    return this;
  }
  public SSHOptions setPrivateKeysFilename(String name) {
    this.privateKeysFilename=name;
    return this;
  }
  public SSHOptions setOpenSSHConfigFilename(String name) {
    this.openSSHConfigFilename=name;
    return this;
  }  
  ////////////////
  // DEBUGGING: //
  ////////////////
  
  public String toString(){
    return "Known Hosts:"+knownHostsFilename
        +"\nPrivate keys:"+privateKeysFilename
        +"\nOpenSSH Config file: "+openSSHConfigFilename+" "
        +"\nDefault Permissions: "+getRWXUser()+getRWXGroup()+getRWXOther()+" "
        +getDefaultFilePermissions()+" "
        +getDefaultDirPermissions()
        ;
  }
  
  ////////////////
  // UTILITIES: //
  ////////////////
  
  private String getPerm(String rwx, boolean value) {
    return value ? rwx :"-";
  }
  private int getDigit(boolean is, int power) {
    return (int) (is ? Math.pow(2, power) :0);
  }
  
  
}
