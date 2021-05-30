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
import org.tmotte.klonk.windows.Positioner;

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
  private JTextField jtfExclude;
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
    boolean needsPos=!initialized;
    init();
    ok=false;

    //Display:
    if (jcbDirData.getSize()>0) {
      String dir=jcbDir.getEditor().getItem().toString();
      if (dir!=null && !"".equals(dir))
        doSearch();
    }
    Point pt=pInfo.parentFrame.getLocation();
    Positioner.set(pInfo.parentFrame, win, !needsPos);
    win.setVisible(true);
    win.paintAll(win.getGraphics());
    win.toFront();
    if (currentFileFinder!=null) {
      currentFileFinder.stop=true;
      currentFileFinder=null;
    }
    if (!ok) {
      return null;
    }
    System.out.println("show() checking results: "+jlFiles.getSelectedValuesList());
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
    saveDir();
    persist.checkSave();
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
    System.out.println("Clicked: "+ok);
    persist.setFileFindExclude(jtfExclude.getText());
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
    if (newDir!=null && newDir.exists() && newDir.isDirectory()){
      jcbDir.getEditor().setItem(newDir.toString());
      saveDir();
    }
  }
  private void dirBoxChanged() {
    System.out.println("Dir box changed, kicking doSearch()");
    doSearch();
  }
  private void saveDir() {
    String newDir=jcbDir.getEditor().getItem().toString();
    System.out.println("\nSTARTING saveDir()"+newDir);
    if (
        newDir==null ||
        newDir.trim().equals("") ||
        !new File(newDir).exists()
      )
      return;
    int index=-1;
    int len=jcbDirData.getSize();
    for (int i=0; i<len && index==-1; i++){
      System.out.println(jcbDirData.getElementAt(i));
      if (jcbDirData.getElementAt(i).equals(newDir))
        index=i;
    }
    if (index==0)
      return;
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
    persist.setFileFindDirs(persistedDirs);
    newDir=null;
  }


  private void doSearch() {
    String dirText=jcbDir.getEditor().getItem().toString();
    if (dirText.trim().equals(""))
      dirText=".";
    File dir=new File(dirText);
    if (!dir.exists() || !dir.isDirectory()) {
      lblError.setVisible(true);
      return;
    }
    lblError.setVisible(false);
    lmFiles.clear();
    foundFileMap.clear();
    if (currentFileFinder!=null) {
      currentFileFinder.stop=true;
    }
    currentFileFinder=new FileFinder(dir, jtfFind.getText(), jtfExclude.getText());
    currentFileFinder.execute();
  }

  private class FileFinder extends SwingWorker<List<String>, String> {
    private final String[][] findParts;
    private final String[][] excludeParts;
    private final File startDir;
    private final boolean caseSensitive=false;
    private String basePath;
    public volatile boolean stop=false;
    public FileFinder(File startDir, String expr, String exclude) {
      if (!caseSensitive){
        expr=expr.toLowerCase().trim();
        exclude=exclude.toLowerCase().trim();
      }
      findParts=makeParts(expr);
      excludeParts=exclude.equals("") ?null :makeParts(exclude);
      this.startDir=startDir;
    }
    private String[][] makeParts(String expr) {
      String[] pieces=expr.split(" ");
      String[][] result=new String[pieces.length][];
      for (int i=0; i<pieces.length; i++)
        result[i]=pieces[i].split("\\*");
      return result;
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
      System.out.println("process search output: "+names);
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
      if (excludeParts!=null && matchesAny(matchingDirName, excludeParts))
        return limit;
      for (File f: dir.listFiles(File::isFile)) {
        if (stop) return 0;
        final String toMatch=matchingDirName+f.getName().toLowerCase();
        final boolean matched=
          matchesAll(toMatch, findParts) &&
          (excludeParts==null || !matchesAny(toMatch, excludeParts));
        if (matched) {
          String name=dirName+f.getName();
          publish(name);
          foundFileMap.put(name, f);
          if (--limit == 0) return 0;
        }
      }
      for (File subdir: dir.listFiles(File::isDirectory))
        if (stop || (limit=find(subdir, limit))<=0)
          return 0;
      return limit;
    }

    private boolean matchesAll(String filename, String[][] parts) {
      for (String[] wildGroup: parts)
        if (!matchInOrder(filename, wildGroup))
          return false;
      return true;
    }
    private boolean matchesAny(String filename, String[][] parts) {
      for (String[] wildGroup: parts)
        if (matchInOrder(filename, wildGroup))
          return true;
      return false;
    }
    private boolean matchInOrder(String filename, String[] pieces) {
      int index=0;
      for (String name: pieces) {
        index=filename.indexOf(name, index);
        if (index==-1)
          return false;
        index+=name.length();
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
    persist.getFileFindDirs(persistedDirs);
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
    jtfExclude=new JTextField();
    jtfExclude.setText(persist.getFileFindExclude());
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
    gb.addY(new JLabel("Find:"));

    gb.fill=gb.HORIZONTAL;
    gb.weightx=1;
    gb.insets.left=5;
    gb.addX(jtfFind);

    // Exclude:
    gb.gridx=1;
    gb.fill=gb.NONE;
    gb.gridwidth=1;
    gb.weightx=0.0;
    gb.insets.left=0;
    gb.insets.top=3;
    gb.addY(new JLabel("Exclude:"));

    gb.fill=gb.HORIZONTAL;
    gb.weightx=1;
    gb.insets.left=5;
    gb.addX(jtfExclude);

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
    // Trigger search when dir box changes:
    jcbDir.addActionListener((ActionEvent event)-> dirBoxChanged());

    // Show directory dialog:
    btnDir.addActionListener((ActionEvent event)-> showDirDialog());
    btnDir.addKeyListener(new KeyAdapter(){
      public @Override void keyReleased(KeyEvent k) {
        if (k.getKeyCode()==KeyEvent.VK_DOWN)
          jtfFind.requestFocusInWindow();
      }
    });

    // Do search when file search box changes:
    DocumentListener docListen=new DocumentListener(){
      public @Override void changedUpdate(DocumentEvent e){doSearch();}
      public @Override void insertUpdate(DocumentEvent e){doSearch();}
      public @Override void removeUpdate(DocumentEvent e){doSearch();}
    };
    jtfFind.getDocument().addDocumentListener(docListen);
    jtfExclude.getDocument().addDocumentListener(docListen);

    // Down arrow from search box hops you to file-select;
    // Enter on search box clicks ok:
    jtfFind.addKeyListener(new KeyAdapter(){
      public @Override void keyReleased(KeyEvent k) {
        if (k.getKeyCode()==KeyEvent.VK_DOWN)
          jtfExclude.requestFocusInWindow();
        else
        if (k.getKeyCode()==KeyEvent.VK_ENTER && lmFiles.size()>0)
          click(true);
      }
    });
    jtfExclude.addKeyListener(new KeyAdapter(){
      public @Override void keyReleased(KeyEvent k) {
        if (k.getKeyCode()==KeyEvent.VK_DOWN)
          jlFiles.requestFocusInWindow();
        else
        if (k.getKeyCode()==KeyEvent.VK_ENTER && lmFiles.size()>0)
          click(true);
      }
    });

    // Press enter in listbox or double-click signals OK:
    jlFiles.addKeyListener(new KeyAdapter(){
      public @Override void keyReleased(KeyEvent k) {
        if (k.getKeyCode()==KeyEvent.VK_ENTER) click(true);
      }
    });
    jlFiles.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2)
          click(true);
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