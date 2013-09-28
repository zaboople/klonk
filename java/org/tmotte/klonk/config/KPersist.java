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
import java.util.Properties;
import org.tmotte.common.swang.Fail;

public class KPersist {
  public static int maxRecent=15;
  public static int maxFavorite=50;

  private File file;
  private Properties properties=new Properties();
  private Fail failer;
  private boolean hasChanges=false;

  private List<String> recentFilesCache, recentDirsCache;
  
  public KPersist(KHome home, Fail failer) {
    this.failer=failer;
    try {
      file=home.nameFile("klonk.properties");
      if (!file.exists())
        file.createNewFile();
      else
        load();
    } catch (Exception e) {
      failer.fail(e); 
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

  // WORD WRAP: //
  
  public KPersist setWordWrap(boolean b) {
    return setBoolean("WordWrap", b);
  }
  public boolean getWordWrap() {
    return getBoolean("WordWrap", true);
  }

  // WINDOW BOUNDS: //

  public KPersist setWindowBounds(Rectangle r) {
    setInt("Window.Top", r.y);
    setInt("Window.Left", r.x);
    setInt("Window.Width", r.width);
    setInt("Window.Height", r.height);
    return this;
  }
  public Rectangle getWindowBounds(Rectangle defR) {
    Rectangle r=new Rectangle();
    r.y=getInt("Window.Top", defR.y);
    r.x=getInt("Window.Left", defR.x);
    r.width=getInt("Window.Width", defR.width);
    r.height=getInt("Window.Height", defR.height);
    return r;
  }
  public KPersist setWindowMaximized(boolean b) {
    return setBoolean("Window.Maximized", b);
  }
  public boolean getWindowMaximized() {
    return getBoolean("Window.Maximized", false);
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

  // LINE DELIMITERS: //

  public String getDefaultLineDelimiter() {
    String s=properties.getProperty("DefaultLineDelimiter");
    if (s==null)          
      return LineDelimiterOptions.CRLFs;
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
    setColor("EditorText.Font.Color", fo.getColor());
    setColor("EditorText.Background", fo.getBackgroundColor());
    setColor("EditorText.Caret.Color", fo.getCaretColor());
    return this;
  }
  public FontOptions getFontAndColors() {
    FontOptions fo=new FontOptions();
    fo.setFontName(
      get("EditorText.Font.Name", fo.getFontName())
    );
    fo.setFontSize(
      getInt("EditorText.Font.Size", fo.getFontSize())
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
    return fo;
  }

  // RECENT FILES & DIRECTORIES & COMMANDS: //

  public KPersist setCommands(List<String> recentCommands){
    setFiles(recentCommands, "File.Batch.", maxRecent);
    hasChanges=true;
    return this;
  }
  public KPersist getCommands(List<String> recentCommands){
    return getFiles(recentCommands, "File.Batch.", maxRecent);
  }
  
  public void getFavorites(List<String> faveFiles,  List<String> faveDirs) {
    getFiles(faveFiles,    "File.Favorite.Files.", maxFavorite);
    getFiles(faveDirs,     "File.Favorite.Dirs." , maxFavorite);
  }
  public void getRecent(List<String> recentFiles,  List<String> recentDirs) {
    getFiles(recentFiles,  "File.RecentFiles.",    maxRecent);
    getFiles(recentDirs,   "File.RecentDirs." ,    maxRecent);
  }
  
  public void setRecentFiles(List<String> files) {
    recentFilesCache=files;
    hasChanges=true;
  }
  public void setRecentDirs(List<String> dirs) {
    recentDirsCache=dirs;
    hasChanges=true;
  }
  public void setFavoriteFiles(List<String> files) {
    setFiles(files, "File.Favorite.Files.", maxFavorite);
  }
  public void setFavoriteDirs(List<String> dirs) {
    setFiles(dirs,  "File.Favorite.Dirs.",  maxFavorite);
  }



  // FAST UNDOS: //
  
  public KPersist setFastUndos(boolean fast) {
    return setBoolean("FastUndos", fast);
  }
  public boolean getFastUndos() {
    return getBoolean("FastUndos", true);
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
    
    // 2. Save everything
    try (FileOutputStream fos=new FileOutputStream(file);) {
      properties.store(fos, "You are permitted to sort this file");
      hasChanges=false;
    } catch (Exception e) {
      failer.fail(e); 
    }
    
  }
  
  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

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
      failer.fail(e);
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
      failer.fail(e);
      return -1;
    }
  }
  
  private void set(String name, String val) {
    properties.setProperty(name, val);
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
    try (FileInputStream fos=new FileInputStream(file);) {
      properties.load(fos);
    } catch (Exception e) {
      failer.fail(e); 
    }
  }
  
}