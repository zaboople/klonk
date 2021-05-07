package org.tmotte.klonk.windows.popup;
import java.awt.Container;
import java.awt.Point;
import java.awt.Color;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.util.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.Radios;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.option.FontOptions;

public class FileFind {
  private final static int maxFiles=30;

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  // DI:
  private final PopupInfo pInfo;
  private final FileDialogWrapper fileDialog;
  private FontOptions fontOptions;
  private KPersist persist;

  // State:
  private boolean ok=false;
  private boolean initialized=false;
  private DefaultComboBoxModel<String> jcbDirData;
  private FileFinder currentFileFinder;
  private Map<String, File> foundFileMap;
  private List<String> persistedDirs;

  // Controls:
  private JDialog win;
  private JButton btnOK, btnCancel, btnDir;
  private JList<String> jlFiles;
  private DefaultListModel<String> lmFiles;
  private JTextField jtfFind;
  private JLabel lblError;
  private JComboBox<String> jcbDir;


  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public FileFind(
      PopupInfo pInfo,
      FontOptions fontOptions,
      KPersist persist,
      FileDialogWrapper fileDialog
    ) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    this.persist=persist;
    this.fileDialog=fileDialog;
    pInfo.addFontListener(fo -> setFont(fo));
  }

  public List<String> show() {
    init();
    ok=false;

    //Display:
    Point pt=pInfo.parentFrame.getLocation();
    win.setLocation(pt.x+20, pt.y+20);//FIXME maintain position
    win.setVisible(true);
    win.paintAll(win.getGraphics());
    win.toFront();
    if (currentFileFinder!=null) {
      currentFileFinder.stop=true;
      currentFileFinder=null;
    }
    if (!ok) {
      foundFileMap.clear();
      return null;
    }
    List<String> results=new ArrayList<>();
    for (String name: jlFiles.getSelectedValuesList()) {
      File file=foundFileMap.get(name);
      if (file!=null)
        try {
          results.add(file.getCanonicalPath());
        } catch (Exception e) {
          throw new RuntimeException("File exploded "+e, e);
        }
    }
    return results;
  }

  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////

  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
  }

  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    ok=action;
    persist.checkSave();
    win.setVisible(false);
  }

  private void showDirDialog(){
    // Search will be automatically triggered because we trigger
    // an action event when jcbDir changes
    File dir=new File(jcbDir.getEditor().getItem().toString());
    if (!dir.exists())
      dir=null;
    File newDir=fileDialog.show(
      new FileDialogWrapper.DialogOpts()
        .setForSave(true)
        .setStartDir(dir)
        .setDirsOnly()
        .setApproveButton("Select")
    );
    if (newDir!=null && newDir.exists() && newDir.isDirectory())
      try {
        saveDir(newDir.getCanonicalPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
  }
  private void dirBoxChanged() {
    doSearch();
  }
  private void saveDir(String newDir) {
    if (newDir.trim().equals(""))
      return;
    int index=-1;
    int len=jcbDirData.getSize();
    for (int i=0; i<len && index==-1; i++)
      if (jcbDirData.getElementAt(i).equals(newDir)){
        index=i;
        break;
      }
    if (index!=-1)
      jcbDirData.removeElementAt(index);
    jcbDirData.insertElementAt(newDir, 0);
    jcbDir.setSelectedIndex(0);
    while (jcbDirData.getSize()>KPersist.maxRecent)
      jcbDirData.removeElementAt(jcbDirData.getSize()-1);
    persistedDirs.clear();
    int size=jcbDirData.getSize();
    for (int i=0; i<size; i++)
      persistedDirs.add(jcbDirData.getElementAt(i));
    persist.setSearchDirs(persistedDirs);
  }


  private void doSearch() {
    String dirText=jcbDir.getEditor().getItem().toString();
    if (dirText.trim().equals(""))
      dirText=".";
    File dir=new File(dirText);
    if (!dir.exists()) {
      lblError.setVisible(true);
      return;
    }
    if (!dirText.equals(".") && !persistedDirs.contains(dirText))
      saveDir(dirText);
    lblError.setVisible(false);
    lmFiles.clear();
    foundFileMap.clear();
    if (currentFileFinder!=null) {
      currentFileFinder.stop=true;
    }
    currentFileFinder=new FileFinder(dir, jtfFind.getText());
    currentFileFinder.execute();
  }

  private class FileFinder extends SwingWorker<List<String>, String> {
    private String[][] parts;
    private String basePath;
    private File startDir;
    private boolean caseSensitive=false;
    public volatile boolean stop=false;
    public FileFinder(File startDir, String expr) {
      if (!caseSensitive)
        expr=expr.toLowerCase();
      String[] pieces=expr.split(" ");
      parts=new String[pieces.length][];
      for (int i=0; i<pieces.length; i++)
        parts[i]=pieces[i].split("\\*");
      this.startDir=startDir;
    }
    public List<String> doInBackground() {
      try {
        basePath=startDir.getCanonicalPath();
        find(startDir, maxFiles);
        return null;
      } catch (IOException e) {
        return null;
      }
    }
    public void process(List<String> names) {
      if (stop)
        return;
      boolean wasEmpty=lmFiles.size()==0;
      for (String name: names)
        lmFiles.addElement(name);
      if (wasEmpty && lmFiles.size()>0)
        jlFiles.setSelectedIndex(0);
      if (lmFiles.size()==maxFiles)
        lmFiles.addElement("...");
    }
    private int find(File dir, int limit) throws IOException {
      String dirName=dir.getCanonicalPath().replace(basePath, "");
      if (!dirName.endsWith(File.separator))
        dirName+=File.separatorChar;
      String matchingDirName=caseSensitive ?dirName :dirName.toLowerCase();
      if (stop) return 0;
      for (File f: dir.listFiles())
        if (f.isFile()){
          String matchingFileName=f.getName();
          if (!caseSensitive)
            matchingFileName=matchingFileName.toLowerCase();
          if (matches(matchingDirName+matchingFileName)) {
            String name=dirName+f.getName();
            publish(name);
            foundFileMap.put(name, f);
            if (--limit == 0 || stop) return 0;
          }
        }
      if (stop) return 0;
      for (File subdir: dir.listFiles(File::isDirectory))
        if (stop || (limit=find(subdir, limit))<=0)
          return 0;
      return limit;
    }
    private boolean matches(String filename) {
      for (String[] sequence: parts) {
        int index=0;
        for (String name: sequence) {
          index=filename.indexOf(name, index);
          if (index==-1)
            return false;
          index+=name.length();
        }
      }
      return true;
    }
  }



  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////

  private void init() {
    if (!initialized) {
      create();
      layout();
      listen();
      initialized=true;
    }
  }
  private void create() {
    win=new JDialog(pInfo.parentFrame, true);
    win.setTitle("Find file");
    persistedDirs=new ArrayList<>();
    persist.getSearchDirs(persistedDirs);
    jcbDirData=new DefaultComboBoxModel<>();
    jcbDirData.removeAllElements();
    for (String f: persistedDirs)
      jcbDirData.addElement(f);
    jcbDir=new JComboBox<>(jcbDirData);
    jcbDir.setEditable(true);
    jcbDir.setMaximumRowCount(KPersist.maxRecent);

    lblError=new JLabel("<html><b>Invalid directory</b></html>");
    lblError.setForeground(Color.RED);
    jtfFind=new JTextField();
    lmFiles=new DefaultListModel<>();
    jlFiles=new JList<>(lmFiles);
    jlFiles.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
    btnDir=new JButton("...");
    btnDir.setMnemonic(KeyEvent.VK_PERIOD);
    foundFileMap=new HashMap<>();
  }


  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightx=1;
    gb.fill=gb.HORIZONTAL;
    gb.anchor=gb.NORTHWEST;
    gb.setInsets(5);
    gb.add(new JLabel("<html><b>Find files</b></html>"));


    gb.fill=gb.BOTH;
    gb.weighty=0.1;
    gb.addY(getDirFilePanel());

    gb.fill=gb.BOTH;
    gb.weighty=1;
    gb.addY(new JScrollPane(jlFiles));

    gb.weightXY(0.0);
    gb.fill=gb.NONE;
    gb.addY(getButtonPanel());

    setFont(fontOptions);
    win.setSize(400, 400);
  }
  private Container getDirFilePanel(){
    GridBug gb=new GridBug(new JPanel());

    // Directory part:
    gb.anchor=gb.WEST;
    gb.add(new JLabel("Directory:"));

    gb.fill=gb.HORIZONTAL;
    gb.weighty=0.0;
    gb.weightx=1.0;
    gb.insets.left=5;
    gb.addX(getDirectoryPanel());

    gb.gridx=1;
    gb.gridwidth=2;
    gb.addY(lblError);
    lblError.setVisible(false);

    // Expression part:
    gb.fill=gb.NONE;
    gb.gridwidth=1;
    gb.weightx=0.0;
    gb.insets.left=0;
    gb.insets.top=3;
    gb.addY(new JLabel("Expression:"));

    gb.fill=gb.HORIZONTAL;
    gb.weightx=1;
    gb.insets.left=5;
    gb.addX(jtfFind);

    return gb.container;
  }

  private Container getDirectoryPanel()  {
    GridBug gb=new GridBug(new JPanel());
    gb.fill=gb.HORIZONTAL;
    gb.weightx=1.0;
    gb.add(jcbDir);
    gb.insets.left=3;
    gb.fill=gb.NONE;
    gb.weightx=0.0;
    gb.addX(btnDir);
    return gb.container;
  }
  private Container getButtonPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.setInsets(5);

    gb.gridx=0;
    gb.add(btnOK);
    gb.addX(btnCancel);
    return gb.container;
  }


  private void listen() {

    // Do search when file search box changes:
    jtfFind.getDocument().addDocumentListener(new DocumentListener(){
      public void changedUpdate(DocumentEvent e){doSearch();}
      public void insertUpdate(DocumentEvent e){doSearch();}
      public void removeUpdate(DocumentEvent e){doSearch();}
    });

    // Down arrow from search box hops you to file-select;
    // Enter on search box clicks ok:
    jtfFind.addKeyListener(new KeyAdapter(){
      public void keyReleased(KeyEvent k) {
        if (k.getKeyCode()==KeyEvent.VK_DOWN)
          jlFiles.requestFocusInWindow();
        else
        if (k.getKeyCode()==KeyEvent.VK_ENTER && lmFiles.size()>0)
          click(true);
      }
    });

    // Trigger search when dir box changes:
    jcbDir.addActionListener((ActionEvent event)-> dirBoxChanged());

    // Show directory dialog:
    btnDir.addActionListener((ActionEvent event)-> showDirDialog());

    // Press enter in listbox clicks OK:
    jlFiles.addKeyListener(new KeyAdapter(){
      public void keyReleased(KeyEvent k) {
        if (k.getKeyCode()==KeyEvent.VK_ENTER) click(true);
      }
    });

    // OK & cancel:
    btnOK.addActionListener((ActionEvent event)-> click(true));
    ActionListener alCancel=(ActionEvent event)-> click(false);
    btnCancel.addActionListener(alCancel);
    KeyMapper.easyCancel(btnCancel, alCancel);
  }

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        PopupTestContext ptc=new PopupTestContext();
        Object ok=new FileFind(
            ptc.getPopupInfo(), ptc.getFontOptions(), ptc.getPersist(),
            new FileDialogWrapper(ptc.getPopupInfo())
          ).show();
        System.out.println(ok);
     }
    });
  }

}