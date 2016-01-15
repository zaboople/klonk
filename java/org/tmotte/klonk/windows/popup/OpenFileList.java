package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JWindow;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.SimpleClipboard;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.Positioner;

public class OpenFileList {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  // DI:
  private CurrentOS currentOS;
  private JFrame parentFrame;
  private FontOptions fontOptions;
  private final Setter<FontOptions> fontListener=new Setter<FontOptions>(){
    public void set(FontOptions fo){setFont(fo);}
  };

  // Controls:
  private JDialog win;
  private MyTextArea mtaFiles;
  private JButton btnOK, btnCancel;
  private Font fontBold;

  // State:
  private boolean initialized=false;
  private boolean result;


  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public OpenFileList(
      JFrame parentFrame,
      FontOptions fontOptions,
      CurrentOS currentOS
    ) {
    this.parentFrame=parentFrame;
    this.fontOptions=fontOptions;
    this.currentOS=currentOS;
  }
  public Setter<FontOptions> getFontListener() {
    return fontListener;
  }
  public List<String> show() {
    init();
    result=false;
    String s=SimpleClipboard.get();
    if (!s.equals("")){
      mtaFiles.betterReplaceRange(s, 0, mtaFiles.getText().length());
      btnOK.requestFocusInWindow();
    }
    Positioner.set(parentFrame, win, false);
    win.setVisible(true);
    win.toFront();
    List<String> files=null;
    if (result) {
      files=new java.util.ArrayList<>(20);
      save(mtaFiles, files);
    }
    return files==null || files.size()==0
      ?null
      :files;
  }

  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////

  private void setFont(FontOptions f) {
    this.fontOptions=f;
    if (initialized) {
      setFont(mtaFiles);
      //Makes the mta assert its designated row count:
      win.pack();
    }
  }
  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    win.setVisible(false);
    result=action;
  }
  private void save(MyTextArea mta, Collection<String> names) {
    boolean first=true;
    names.clear();
    try {
      int size=mta.getLineCount();
      for (int i=0; i<size; i++){
        int start=mta.getLineStartOffset(i),
            end=mta.getLineEndOffset(i);
        String name=mta.getText(start, end-start).trim();
        if (!name.equals(""))
          names.add(name);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setFont(MyTextArea mta) {
    FontOptions f=fontOptions;
    mta.setFont(f.getFont());
    mta.setForeground(f.getColor());
    mta.setBackground(f.getBackgroundColor());
    mta.setCaretColor(f.getCaretColor());
  }

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////

  private void init() {
    if (!initialized){
      create();
      layout();
      listen();
      initialized=true;
    }
  }
  private void create(){
    win=new JDialog(parentFrame, true);
    win.setTitle("Open filenames from list");
    fontBold=new JLabel().getFont().deriveFont(Font.BOLD);
    mtaFiles=getMTA();
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
    setFont(mtaFiles);
  }
  private MyTextArea getMTA(){
    MyTextArea mta=new MyTextArea(currentOS);
    mta.setRows(7);
    mta.setColumns(60);
    mta.setLineWrap(false);
    mta.setWrapStyleWord(false);
    mta.setFont(fontOptions.getFont());
    return mta;
  }

  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.add(getInputPanel());

    gb.weightXY(1,0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(getButtons());
    win.pack();
  }
  private Container getInputPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);
    gb.weightXY(0);

    gb.anchor=gb.CENTER;
    gb.insets.top=5;
    gb.insets.bottom=5;
    gb.insets.left=5;
    gb.insets.right=5;
    {
      JLabel j=new JLabel("File names will be automatically be pasted below from the clipboard:");
      Font f=j.getFont();
      f=f.deriveFont(Font.BOLD, f.getSize()+1);
      j.setFont(f);
      gb.addY(j);
    }

    gb.anchor=gb.WEST;

    gb.insets.top=0;
    gb.insets.left=0;
    gb.insets.right=0;
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.anchor=gb.NORTH;
    gb.addY(mtaFiles.makeVerticalScrollable());

    return gb.container;
  }
  private Container getButtons() {
    GridBug gb=new GridBug(new JPanel());
    Insets insets=gb.insets;
    insets.top=5;
    insets.bottom=5;
    insets.left=5;
    insets.right=5;

    gb.gridx=0;
    gb.add(btnOK);
    gb.addX(btnCancel);
    return gb.container;
  }
  private void listen() {

    mtaFiles.addKeyListener(new MTAKeyListen(mtaFiles, btnCancel, btnOK));

    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(true);}
    };
    btnOK.addActionListener(okAction);

    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.easyCancel(btnCancel, cancelAction);
  }

  private class MTAKeyListen extends  KeyAdapter {
    JComponent prev, next;
    MyTextArea mta;
    public MTAKeyListen(MyTextArea mta, JComponent prev, JComponent next){
      this.mta=mta;
      this.prev=prev;
      this.next=next;
    }
    public void keyPressed(KeyEvent e){
      final int code=e.getKeyCode();
      if (code==e.VK_TAB) {
        int mods=e.getModifiersEx();
        if (KeyMapper.ctrlPressed(mods))
          mta.replaceSelection("	");
        else
        if (KeyMapper.shiftPressed(mods))
          prev.requestFocusInWindow();
        else
          next.requestFocusInWindow();
        e.consume();
      }
    }
  };

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      PopupTestContext ptc=new PopupTestContext();
      public void run() {
        List<String> files=
          new OpenFileList(
            ptc.makeMainFrame(),
            new FontOptions(),
            ptc.getCurrentOS()
          ).show();
        System.out.println();
        if (files!=null)
          for (String s: files)
            System.out.println("FILE: "+s);
      }
    });
  }

}
