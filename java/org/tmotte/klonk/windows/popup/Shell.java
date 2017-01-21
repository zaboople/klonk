package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.text.StackTracer;
import org.tmotte.common.text.StringChunker;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.Positioner;

public class Shell {

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;
  private FileDialogWrapper fdw;
  private KPersist persist;
  private Getter<String> currFileGetter;
  private Image icon;

  // Controls:
  private JFrame win;
  private MyTextArea mtaOutput;
  private JButton btnRun, btnClose, btnSwitch, btnSelectFile, btnForgetFile, btnStop;
  private JComboBox<String> jcbPrevious;
  private Font fontBold;

  // State:
  private boolean shownBefore=false;
  private List<String> persistedFiles;
  private boolean initialized=false;
  private DefaultComboBoxModel<String> jcbPreviousData;

  public Shell(
     PopupInfo pInfo,
     FontOptions fontOptions,
     KPersist persist,
     FileDialogWrapper fdw,
     Image icon,
     Getter<String> currFileGetter
    ) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    this.fdw=fdw;
    this.persist=persist;
    this.icon=icon;
    this.currFileGetter=currFileGetter;
    pInfo.addFontListener(fo -> setFont(fo));
  }
  public void show() {
    init();
    Positioner.set(pInfo.parentFrame, win, shownBefore || (win.getBounds().x>-1 && win.getBounds().y>-1));
    shownBefore=true;
    win.setVisible(true);
    win.toFront();
  }

  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////

  ////////////////////////
  // WINDOW STATE MGMT: //
  ////////////////////////

  private void showFileDialog() {
    File file=new File(jcbPrevious.getEditor().getItem().toString());
    file=fdw.show(false, file, null);
    if (file!=null)
      try {
        jcbPrevious.getEditor().setItem(file.getCanonicalPath());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
  }
  private void closing() {
    preserveBounds();
    persist.checkSave();
  }
  private void close() {
    closing();
    win.setVisible(false);
  }
  private void switchBack() {
    preserveBounds();
    pInfo.parentFrame.toFront();
  }
  private void preserveBounds() {
    persist.setShellWindowBounds(win.getBounds());
  }
  private void setFont(FontOptions f) {
    this.fontOptions=f;
    if (win!=null) {
      f.getControlsFont().set(win);
      mtaOutput.setFont(f.getFont());
      mtaOutput.setForeground(f.getColor());
      mtaOutput.setBackground(f.getBackgroundColor());
      mtaOutput.setCaretColor(f.getCaretColor());
      mtaOutput.setCaretWidth(f.getCaretWidth());
      win.pack();//To make rowcount work
    }
  }

  ///////////////////////////////
  // COMMAND STORE & RETRIEVE: //
  ///////////////////////////////

  private void forget() {
    int index=getIndexOf(
      jcbPrevious.getEditor().getItem().toString()
    );
    if (index!=-1){
      jcbPreviousData.removeElementAt(index);
      save(jcbPreviousData, persistedFiles);
    }
    jcbPrevious.requestFocusInWindow();
  }
  private int getIndexOf(String cmd) {
    int index=-1;
    int len=jcbPreviousData.getSize();
    for (int i=0; i<len && index==-1; i++)
      if (jcbPreviousData.getElementAt(i).equals(cmd)){
        index=i;
        break;
      }
    return index;
  }
  private void save(DefaultComboBoxModel<String> lm, List<String> names) {
    while (lm.getSize()>KPersist.maxFavorite)
      lm.removeElementAt(lm.getSize()-1);
    names.clear();
    int size=lm.getSize();
    for (int i=0; i<size; i++)
      names.add(lm.getElementAt(i));
    persist.setCommands(names);
  }
  private void saveCommand(String cmd) {
    int index=getIndexOf(cmd);
    if (index!=-1)
      jcbPreviousData.removeElementAt(index);
    jcbPreviousData.insertElementAt(cmd, 0);
    jcbPrevious.setSelectedIndex(0);
    save(jcbPreviousData, persistedFiles);
  }
  private String getCommand() {
    return jcbPrevious.getEditor().getItem().toString();
  }


  ////////////////////////
  // COMMAND EXECUTION: //
  ////////////////////////

  private void exec() {
    die();
    btnRun.setEnabled(false);
    btnStop.setEnabled(true);
    String command=getCommand();
    mtaOutput.reset();
    runner=new Runner2(command);
    runner.execute();
  }
  private void die(){
    btnStop.setEnabled(false);
    btnRun.setEnabled(true);
    if (runner!=null)
      runner.die();
  }
  private Runner2 runner;
  class Runner2 extends SwingWorker<Void, String> {
    private String input;
    private boolean kill=false, failed=false;
    private Process process;
    public Runner2(String input) {
      this.input=input;
    }
    public void die() {
      //This only works so good. I noticed that starting a java program
      //could not be killed. It only dies when it wants to.
      kill=true;
      try {
        if (process!=null)
          process.destroy();
      } catch (Exception e) {e.printStackTrace();}
    }
    @Override public Void doInBackground() {
      //Do NOT reference the GUI here; do not under ANY circumstances
      //touch any Swing component whatsoever. Leave that for process() & done().
      kill=false;
      try {
        List<String> commands=ShellCommandParser.parse(input, currFileGetter.get());
        for (int i=0; i<commands.size(); i++)
          if (i==0)
            publish("Command: "+commands.get(i)+"\n");
          else
            publish("  Parameter: "+commands.get(i)+"\n");
        ProcessBuilder pb=new ProcessBuilder(commands);
        pb.redirectErrorStream(true);
        process=pb.start();
        try (
            InputStream istr=process.getInputStream();
            InputStreamReader isr=new InputStreamReader(istr);
          ){
          int charsRead=0;
          char[] readBuffer=new char[1024 * 64];
          while (!kill && (charsRead=isr.read(readBuffer, 0, readBuffer.length))>0)
            publish(new String(readBuffer, 0, charsRead));
        }
      } catch (Exception e) {
        failed=true;
        publish(StackTracer.getStackTrace(e).replaceAll("\t", "    "));
      } finally {
      }
      return null;
    }
    @Override protected void process(List<String> lines){
      if (!kill)
        for (String s: lines)
          mtaOutput.append(s);
    }
    @Override protected void done() {
      btnRun.setEnabled(true);
      btnStop.setEnabled(false);
      if (!failed)
        saveCommand(input);
      if (!kill)
        mtaOutput.requestFocusInWindow();
    }

  };


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
  private void create(){
    jcbPreviousData=new DefaultComboBoxModel<>();
    fontBold=new JLabel().getFont().deriveFont(Font.BOLD);
    persistedFiles=new LinkedList<>();
    persist.getCommands(persistedFiles);

    String hStart="<html><body>",
           hEnd="</body></html>",
           bStart="<b>",
           bEnd="</b>";

    win=new JFrame("Klonk: Run batch program:");
    win.setIconImage(icon);
    icon=null;//Don't need it after this

    btnSelectFile=new JButton("File...");
    btnSelectFile.setMnemonic(KeyEvent.VK_F);

    btnForgetFile=new JButton("Forget");
    btnForgetFile.setMnemonic(KeyEvent.VK_O);

    jcbPreviousData.removeAllElements();
    for (String f: persistedFiles)
      jcbPreviousData.addElement(f);
    jcbPrevious=new JComboBox<>(jcbPreviousData);
    jcbPrevious.setEditable(true);
    jcbPrevious.setMaximumRowCount(KPersist.maxFavorite);

    btnRun   =new JButton(
      hStart+bStart
      +"Run "+bEnd+" ("
      +(pInfo.currentOS.isOSX ? "⌘" :"Ctrl")
      +"-E)"
      +hEnd
    );
    btnRun.setMnemonic(KeyEvent.VK_R);

    btnStop   =new JButton("Stop");
    btnStop.setMnemonic(KeyEvent.VK_S);
    btnStop.setFont(fontBold);
    btnStop.setEnabled(false);

    mtaOutput=getMTA();

    btnClose    =new JButton(hStart+bStart+"Close"+bEnd+" (ESC)"+hEnd);
    btnClose.setMnemonic(KeyEvent.VK_C);
    btnSwitch   =new JButton(hStart+bStart+"Back to main window "+bEnd+"(Ctrl-B)"+hEnd);
    btnSwitch.setMnemonic(KeyEvent.VK_B);
  }
  private MyTextArea getMTA(){
    MyTextArea mta=new MyTextArea(pInfo.currentOS);
    mta.setLineWrap(true);
    mta.setWrapStyleWord(false);
    mta.setEditable(true);
    return mta;
  }

  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridXY(0);
    gb.weightXY(1, 0);
    gb.anchor=gb.WEST;
    gb.fill=gb.HORIZONTAL;
    gb.addY(getFileSelectPanel());

    gb.addY(getButtonRunPanel());

    gb.weightXY(1, 1);
    gb.fill=gb.BOTH;
    gb.addY(getOutputPanel());

    gb.weightXY(1,0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(getButtonClosePanel());
    Rectangle r=persist.getShellWindowBounds(
      new Rectangle(-1, -1, 450, 450)
    );

    setFont(fontOptions);

    win.setBounds(r);
    win.setPreferredSize(new Dimension(r.width, r.height));
  }
  private Container getFileSelectPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);

    gb.weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(jcbPrevious);

    gb.weightXY(0, 0);
    gb.insets.left=5;
    gb.fill=gb.NONE;
    gb.addX(btnSelectFile);
    gb.addX(btnForgetFile);

    return gb.container;
  }
  private Container getButtonRunPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);
    gb.weightXY(0);
    gb.insets.top=5;
    gb.insets.bottom=5;
    gb.insets.left=10;
    gb.insets.right=10;
    gb.addX(btnRun);
    gb.addX(btnStop);
    return gb.container;
  }
  private Container getOutputPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);
    gb.weightXY(0);
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.add(mtaOutput.makeVerticalScrollable());
    return gb.container;
  }
  private Container getButtonClosePanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);
    gb.weightXY(0);
    gb.insets.top=5;
    gb.insets.right=12;
    gb.insets.bottom=5;
    gb.addX(btnClose);
    gb.insets.right=0;
    gb.addX(btnSwitch);
    return gb.container;
  }

  private void listen(){

    //Alt-D focuses combobox... this probably shouldn't be
    //attached to btnRun but ok it works:
    Action actionJCB=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        jcbPrevious.requestFocusInWindow();
        jcbPrevious.getEditor().selectAll();
      }
    };
    KeyMapper.accel(
      jcbPrevious, actionJCB,
      pInfo.currentOS.isOSX
        ?KeyMapper.key(KeyEvent.VK_L, KeyMapper.shortcutByOS())
        :KeyMapper.key(KeyEvent.VK_D, KeyEvent.ALT_DOWN_MASK)
    );

    //File:
    Action fileAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {showFileDialog();}
    };
    btnSelectFile.addActionListener(fileAction);
    pInfo.currentOS.fixEnterKey(btnSelectFile, fileAction);

    //Forget:
    Action forgetAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {forget();}
    };
    btnForgetFile.addActionListener(forgetAction);
    pInfo.currentOS.fixEnterKey(btnForgetFile, forgetAction);

    //Run:
    Action runAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {exec();}
    };
    btnRun.addActionListener(runAction);
    pInfo.currentOS.fixEnterKey(btnRun, runAction);

    // Hotwire run to modifier-E everywhere:
    KeyMapper.accel(
      btnRun, runAction,
      KeyMapper.key(KeyEvent.VK_E, KeyMapper.shortcutByOS())
    );
    jcbPrevious.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e){
        final int code=e.getKeyCode();
        if (code==e.VK_ENTER && !jcbPrevious.isPopupVisible() && btnRun.isEnabled()) {
          exec();
          e.consume();
        }
      }
    });

    //Stop:
    Action stopAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        try {die(); } catch (Exception e) {}
      }
    };
    btnStop.addActionListener(stopAction);
    pInfo.currentOS.fixEnterKey(btnStop, stopAction);

    //Output:
    mtaOutput.addKeyListener(textAreaListener);

    //Close:
    Action closeAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {close();}
    };
    btnClose.addActionListener(closeAction);
    pInfo.currentOS.fixEnterKey(btnClose, closeAction);
    KeyMapper.easyCancel(btnClose, closeAction);
    win.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        closing();
      }
    });

    //Switch back:
    Action switchAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {switchBack();}
    };
    KeyMapper.accel(
      btnSwitch, switchAction, KeyMapper.key(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK)
    );
    btnSwitch.addActionListener(switchAction);
    pInfo.currentOS.fixEnterKey(btnSwitch, switchAction);
  }
  private KeyAdapter textAreaListener=new KeyAdapter() {
    public void keyPressed(KeyEvent e){
      final int code=e.getKeyCode();
      if (code==e.VK_TAB) {
        int mods=e.getModifiersEx();
        if (KeyMapper.shiftPressed(mods))
          btnRun.requestFocusInWindow();
        else
          btnClose.requestFocusInWindow();
        e.consume();
      }
    }
  };


  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {

    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        PopupTestContext ptc=new PopupTestContext();
        Shell shell=new Shell(
          ptc.getPopupInfo(), ptc.getFontOptions(), ptc.getPersist(),
          new FileDialogWrapper(ptc.getPopupInfo()),
          ptc.getPopupIcon(),
          new Getter<String>() {
            public String get() {return null;}
          }
        );
        shell.show();
        //Won't work because previous command is not synchronous
        //ptc.getPersist().save();
      }
    });
  }

}