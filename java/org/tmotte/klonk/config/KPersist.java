package org.tmotte.klonk.config;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.UserServer;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.klonk.config.option.TabAndIndentOptions;
import org.tmotte.klonk.config.option.LineDelimiterOptions;

public class KPersist {
  public final static int maxRecent=15;
  public final static int maxFavorite=50;

  private Setter<Throwable> logFail;
  private CurrentOS currentOS;

  private File mainFile;
  private Properties properties=new Properties();
  private boolean hasChanges=false;

  private FontOptions fontOptionsCache;
  private SSHOptions sshOptionsCache;
  private List<String> recentFilesCache, recentDirsCache;
  private List<UserServer> recentSSHCache;
  private KeyConfig keyConfig;

  public KPersist(KHome home, Setter<Throwable> logFail, CurrentOS currentOS) {
    this.logFail=logFail;
    this.currentOS=currentOS;
    try {
      mainFile=home.nameFile("klonk.properties");
      if (!mainFile.exists())
        mainFile.createNewFile();
      else
        load();
    } catch (Exception e) {
      logFail.set(e);
    }
  }



  /////////////////
  // PROPERTIES: //
  /////////////////

  public String debug(){
    StringBuilder sb=new StringBuilder();
    for (Object s: properties.keySet()){
      sb.append("\n"+s+"-->"+properties.get(s));
    }
    return sb.toString();
  }

  // SIMPLE BOOLEANS: WORD WRAP, FAST UNDOS, AUTO-TRIM: //

  public KPersist setWordWrap(boolean b) {
    return setBoolean("WordWrap", b);
  }
  public boolean getWordWrap() {
    return getBoolean("WordWrap", true);
  }

  public KPersist setFastUndos(boolean fast) {
    return setBoolean("FastUndos", fast);
  }
  public boolean getFastUndos() {
    return getBoolean("FastUndos", true);
  }

  public KPersist setAutoTrim(boolean trim) {
    return setBoolean("AutoTrim", trim);
  }
  public boolean getAutoTrim() {
    return getBoolean("AutoTrim", false);
  }

  // WINDOW BOUNDS: //

  public KPersist setWindowBounds(Rectangle r) {
    setBounds("Window", r);
    return this;
  }
  public Rectangle getWindowBounds(Rectangle defR) {
    return getBounds("Window", defR);
  }
  public KPersist setWindowMaximized(boolean b) {
    return setBoolean("Window.Maximized", b);
  }
  public boolean getWindowMaximized() {
    return getBoolean("Window.Maximized", false);
  }

  public void setShellWindowBounds(Rectangle r) {
    setBounds("Shell.Window", r);
  }
  public Rectangle getShellWindowBounds(Rectangle defR) {
    return getBounds("Shell.Window", defR);
  }


  // TABS AND INDENTS: //

  public TabAndIndentOptions getTabAndIndentOptions(){
    TabAndIndentOptions taio=new TabAndIndentOptions();
    taio.indentOnHardReturn=getBoolean("Indent.OnHardReturn", true);
    taio.tabIndentsLine    =getBoolean("Indent.TabIndentsLine", true);
    String temp=get("Indent.DefaultMode", "SPACES");
    if ("TABS".equals(temp))
      taio.indentionModeDefault=taio.INDENT_TABS;
    else
      taio.indentionModeDefault=taio.INDENT_SPACES;
    taio.indentSpacesSize=getInt("Indent.SpacesSize", 2);
    taio.tabSize=getInt("Tabs.Size", 4);
    return taio;
  }
  public void setTabAndIndentOptions(TabAndIndentOptions taio){
    setBoolean("Indent.OnHardReturn", taio.indentOnHardReturn);
    setBoolean("Indent.TabIndentsLine", taio.tabIndentsLine);
    if (taio.indentionModeDefault==taio.INDENT_TABS)
      set("Indent.DefaultMode", "TABS");
    else
      set("Indent.DefaultMode", "SPACES");
    setInt("Indent.SpacesSize", taio.indentSpacesSize);
    setInt("Tabs.Size", taio.tabSize);
  }

  // SSH OPTIONS: //

  public SSHOptions getSSHOptions(){
    if (sshOptionsCache==null) {
      SSHOptions opts=new SSHOptions();
      opts.setKnownHostsFilename(get("SSH.KnownHosts", null));
      opts.setPrivateKeysFilename(get("SSH.PrivateKeys", null));
      opts.setOpenSSHConfigFilename(get("SSH.OpenSSHConfig", null));
      String user=get("SSH.Default.User",  "rw-"),
            group=get("SSH.Default.Group", "r--"),
            other=get("SSH.Default.Other", "---");
      opts.dur=user.contains("r");
      opts.duw=user.contains("w");
      opts.dux=user.contains("x");
      opts.dgr=group.contains("r");
      opts.dgw=group.contains("w");
      opts.dgx=group.contains("x");
      opts.dor=other.contains("r");
      opts.dow=other.contains("w");
      opts.dox=other.contains("x");
      sshOptionsCache=opts;
    }
    return sshOptionsCache;
  }
  public void writeSSHOptions(){
    SSHOptions opts=getSSHOptions();
    set("SSH.KnownHosts",    opts.getKnownHostsFilename());
    set("SSH.PrivateKeys",   opts.getPrivateKeysFilename());
    set("SSH.OpenSSHConfig", opts.getOpenSSHConfigFilename());
    set("SSH.Default.User",  opts.getRWXUser());
    set("SSH.Default.Group", opts.getRWXGroup());
    set("SSH.Default.Other", opts.getRWXOther());
  }

  // LINE DELIMITERS: //

  public String getDefaultLineDelimiter() {
    String s=properties.getProperty("DefaultLineDelimiter");
    if (s==null)
      return
        currentOS.isMSWindows
          ?LineDelimiterOptions.CRLFs
          :LineDelimiterOptions.LFs;
    else
      return LineDelimiterOptions.translateFromReadable(s);
  }
  public KPersist setDefaultLineDelimiter(String option) {
    String s=LineDelimiterOptions.translateToReadable(option);
    set("DefaultLineDelimiter", s);
    return this;
  }

  // FONT & BACKGROUND //
  public KPersist setFontAndColors(FontOptions fo) {
    set("EditorText.Font.Name", fo.getFontName());
    setInt("EditorText.Font.Size", fo.getFontSize());
    setInt("EditorText.Font.Style", fo.getFontStyle());
    setColor("EditorText.Font.Color", fo.getColor());
    setColor("EditorText.Background", fo.getBackgroundColor());
    setColor("EditorText.Caret.Color", fo.getCaretColor());
    setInt("EditorText.Caret.Width", fo.getCaretWidth());
    setInt("Window.Controls.Minimum.Font.Size", fo.getControlsFont().getSize());

    fontOptionsCache=null;
    getFontAndColors();
    return this;
  }
  public FontOptions getFontAndColors() {
    if (fontOptionsCache==null) {
      fontOptionsCache=new FontOptions();
      FontOptions fo=fontOptionsCache;
      fo.setFontName(
        get("EditorText.Font.Name", fo.getFontName())
      );
      fo.setFontSize(
        getInt("EditorText.Font.Size", fo.getFontSize())
      );
      fo.setFontStyle(
        getInt("EditorText.Font.Style", fo.getFontStyle())
      );
      fo.setColor(
        getColor("EditorText.Font.Color", fo.getColor())
      );
      fo.setBackgroundColor(
        getColor("EditorText.Background", fo.getBackgroundColor())
      );
      fo.setCaretColor(
        getColor("EditorText.Caret.Color", fo.getCaretColor())
      );
      fo.setCaretWidth(
        getInt("EditorText.Caret.Width", fo.getCaretWidth())
      );
      fo.setControlsFont(
        getInt("Window.Controls.Minimum.Font.Size", 12)
      );
    }
    return fontOptionsCache;
  }

  // RECENT FILES & DIRECTORIES & COMMANDS: //

  public KPersist setCommands(List<String> recentCommands){
    setFiles(recentCommands, "File.Batch.", maxRecent);
    return this;
  }
  public KPersist getCommands(List<String> recentCommands){
    return getFiles(recentCommands, "File.Batch.", maxRecent);
  }

  public void getRecent(List<String> recentFiles,  List<String> recentDirs) {
    getFiles(recentFiles,  "File.RecentFiles.",    maxRecent);
    getFiles(recentDirs,   "File.RecentDirs." ,    maxRecent);
  }
  public void setRecentFiles(List<String> files) {
    recentFilesCache=files;
  }
  public void setRecentDirs(List<String> dirs) {
    recentDirsCache=dirs;
  }

  public void getRecentSSH(List<UserServer> recent) {
    recent.clear();
    List<String> r=new ArrayList<>(maxRecent);
    getFiles(r,  "File.Recent.SSH.", maxRecent);
    for (String item: r) {
      String[] s=item.split("@");
      if (s.length != 2)
        throw new RuntimeException("File.Recent.SSH.## is corrupted, should be name@server on each entry, check your home directory");
      recent.add(new UserServer(s[0], s[1]));
    }
  }
  public void setRecentSSH(List<UserServer> sshs) {
    recentSSHCache=sshs;
  }

  public void getFavorites(List<String> faveFiles,  List<String> faveDirs) {
    getFiles(faveFiles,    "File.Favorite.Files.", maxFavorite);
    getFiles(faveDirs,     "File.Favorite.Dirs." , maxFavorite);
  }
  public void setFavoriteFiles(List<String> files) {
    setFiles(files, "File.Favorite.Files.", maxFavorite);
  }
  public void setFavoriteDirs(List<String> dirs) {
    setFiles(dirs,  "File.Favorite.Dirs.",  maxFavorite);
  }

  public void setEncryptionShowPass(boolean yes) {
    setBoolean("Encryption.Show.Pass", yes);
  }
  public boolean getEncryptionShowPass() {
    return getBoolean("Encryption.Show.Pass", false);
  }
  public void setEncryptionPreservePass(boolean yes) {
    setBoolean("Encryption.Preserve.Pass", yes);
  }
  public boolean getEncryptionPreservePass() {
    return getBoolean("Encryption.Preserve.Pass", false);
  }
  public void setEncryptionBits(int bits) {
    setInt("Encryption.Bits", bits);
  }
  public int getEncryptionBits() {
    return getInt("Encryption.Bits", 128);
  }

  // SHORTCUTS: //

  public KeyConfig getKeyConfig() {
    if (keyConfig==null)
      try {
        keyConfig=new KeyConfig(properties);
      } catch (Exception e) {
        throw new RuntimeException("Key config failed to load: "+e.getMessage(), e);
      }
    return keyConfig;
  }

  ///////////
  // SAVE: //
  ///////////

  /** This is for when we do lazy save, because some things change too quickly to
      be constantly saving.*/
  public KPersist checkSave() {
    if (hasChanges)
      save();
    return this;
  }
  public void save() {

    // 1. Check the file caches and make properties:
    if (recentFilesCache!=null) {
      setFiles(recentFilesCache,   "File.RecentFiles.",    maxRecent);
      recentFilesCache=null;
    }
    if (recentDirsCache!=null) {
      setFiles(recentDirsCache,    "File.RecentDirs.",     maxRecent);
      recentDirsCache=null;
    }
    if (recentSSHCache!=null) {
      List<String> r=new ArrayList<>(recentSSHCache.size());
      for (UserServer us : recentSSHCache)
        r.add(us.user+"@"+us.server);
      setFiles(r,    "File.Recent.SSH.",     maxRecent);
      recentSSHCache=null;
    }

    // 2. Save everything
    try (FileOutputStream fos=new FileOutputStream(mainFile);) {
      properties.store(fos, "You are permitted to sort this file");
      hasChanges=false;
    } catch (Exception e) {
      logFail.set(e);
    }

  }

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void setBounds(String name, Rectangle r) {
    setInt(name+".Top", r.y);
    setInt(name+".Left", r.x);
    setInt(name+".Width", r.width);
    setInt(name+".Height", r.height);
  }
  private Rectangle getBounds(String name, Rectangle defR) {
    Rectangle r=new Rectangle();
    r.y     =getInt(name+".Top", defR.y);
    r.x     =getInt(name+".Left", defR.x);
    r.width =getInt(name+".Width", defR.width);
    r.height=getInt(name+".Height", defR.height);
    return r;
  }

  private void setFiles(List<String> files, String name, int max) {
    int len=files.size();
    int maxlen=max==-1
      ?len
      :Math.min(len, max);
    for (int i=0; i<maxlen; i++)
      set(name+i, files.get(i));
    for (int i=maxlen; i<max; i++)
      properties.remove(name+i);
    return;
  }
  private KPersist getFiles(List<String> files, String name, int max) {
    for (int i=0; i<max; i++){
      String s=properties.getProperty(name+i);
      if (s!=null)
        files.add(s);
    }
    return this;
  }

  private KPersist setColor(String name, java.awt.Color c) {
    setInt(name+".R", c.getRed());
    setInt(name+".G", c.getGreen());
    setInt(name+".B", c.getBlue());
    return this;
  }
  private Color getColor(String name, java.awt.Color defaultTo) {
    int r=getInt(name+".R", defaultTo.getRed());
    int g=getInt(name+".G", defaultTo.getGreen());
    int b=getInt(name+".B", defaultTo.getBlue());
    if (r<0 || r>255) r=100;
    if (g<0 || g>255) g=100;
    if (b<0 || b>255) b=100;
    return new Color(r, g, b);
  }


  private KPersist setBoolean(String name, boolean val) {
    set(name, String.valueOf(val));
    return this;
  }
  private boolean getBoolean(String name, boolean defaultTo) {
    String s=properties.getProperty(name);
    if (s==null)
      return defaultTo;
    try {
      return Boolean.parseBoolean(s);
    } catch (Exception e) {
      logFail.set(e);
      return false;
    }
  }

  private void setInt(String name, int val) {
    set(name, String.valueOf(val));
  }
  private int getInt(String name, int defaultTo) {
    String s=properties.getProperty(name);
    if (s==null)
      return defaultTo;
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      logFail.set(e);
      return -1;
    }
  }

  private void set(String name, String val) {
    properties.setProperty(name, val);
    hasChanges=true;
  }
  private String get(String name) {
    return properties.getProperty(name);
  }
  private String get(String name, String defaultVal) {
    String val=get(name);
    if (val==null || val.equals(""))
      val=defaultVal;
    return val;
  }

  private void load() {
    try (FileInputStream fos=new FileInputStream(mainFile);) {
      properties.load(fos);
    } catch (Exception e) {
      logFail.set(e);
    }
  }

}