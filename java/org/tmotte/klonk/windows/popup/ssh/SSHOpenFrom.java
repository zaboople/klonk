package org.tmotte.klonk.windows.popup.ssh;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.UserServer;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.windows.Positioner;
import org.tmotte.klonk.windows.popup.PopupTestContext;

public class SSHOpenFrom {

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;

  // Controls:
  private JDialog win;
  private JComboBox<String> jcbPrevious;
  private JCheckBox jcbSudo;
  private DefaultComboBoxModel<String> jcbPreviousData;
  private JTextField jtfFile;
  private JButton btnOK, btnCancel;
  private JLabel lblError;
  private JPanel pnlError;

  // State:
  private boolean shownBefore=false;
  private boolean initialized=false;
  private boolean failed=false;
  private SSHOpenFromResult result=null;

  public SSHOpenFrom(PopupInfo pInfo, FontOptions fontOptions) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    pInfo.addFontListener(fo -> setFont(fo));
  }

  private void doShow() {
    win.setVisible(true);
    win.toFront();
  }

  public SSHOpenFromResult show(boolean forSave, List<UserServer> recent) {
    init();

    //Dynamic title:
    win.setTitle(forSave ?"Save to SHH" :"Open from SSH");

    //Sudo always off:
    jcbSudo.setVisible(false);
    jcbSudo.setSelected(false);

    //Dynamic previous:
    jcbPreviousData.removeAllElements();
    if (recent.size()==0)
      jcbPreviousData.addElement("");
    else
      for (UserServer us: recent)
        jcbPreviousData.addElement(us.user+"@"+us.server);
    if (!shownBefore && !jcbPrevious.hasFocus())
      jcbPrevious.requestFocusInWindow();
    if (!shownBefore)
      win.pack();
    Rectangle parentRect=pInfo.parentFrame.getBounds(), thisRect=win.getBounds();
    if (parentRect.width / 2 > thisRect.width){
      thisRect.width=parentRect.width / 2;
      win.setBounds(thisRect);
    }
    Positioner.set(pInfo.parentFrame, win, false);
    failed=true;
    shownBefore=true;
    lblError.setVisible(false);
    lblError.setText(null);
    while (failed)
      doShow();
    return result;
  }



  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    result=null;
    failed=false;
    if (action){
      String userHost=jcbPrevious.getEditor().getItem().toString().trim();
      String file=jtfFile.getText().trim();
      if (!userHost.equals("")) {
        if (file.equals(""))
          file=":~";
        else
        if (!file.startsWith("~") && !file.startsWith("/"))
          file=":~/"+file;
        else
          file=":"+file;
        result=new SSHOpenFromResult("ssh:"+userHost+file, jcbSudo.isSelected());
      }
      else {
        failed=true;
        lblError.setText("ERROR: User@Host is needed");
        lblError.setVisible(true);
      }
    }
    if (!failed)
      win.setVisible(false);
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
  private void create(){
    win=new JDialog(pInfo.parentFrame, true);
    win.setResizable(true);
    win.setTitle("blargh");

    jcbPreviousData=new DefaultComboBoxModel<>();

    jcbPrevious=new JComboBox<>(jcbPreviousData);
    jcbPrevious.setEditable(true);
    jcbPrevious.setMaximumRowCount(KPersist.maxRecent);

    jcbSudo=new JCheckBox("");

    jtfFile=new JTextField();

    pnlError=new JPanel();
    lblError=new JLabel();
    lblError.setText("<none>");
    lblError.setForeground(Color.RED);

    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=gb.NONE;
    gb.anchor=gb.NORTHWEST;
    gb.fill=gb.HORIZONTAL;
    gb.weightXY(1,0);
    gb.insets.right=5;
    gb.add(getInputPanel());
    gb.insets.right=0;
    gb.weightXY(1);
    gb.addY(getButtons());
    gb.weightXY(0);
    gb.addY(getErrorPanel());

    setFont(fontOptions);
  }
  private JPanel getInputPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    gb.insets.top=5;
    gb.insets.bottom=2;
    gb.insets.left=10;
    gb.add(new JLabel(""));
    gb.addX(new JLabel("User@Host"));
    gb.addX(new JLabel("File"));
    //gb.addX(new JLabel("Sudo"));
    gb.addX(new JLabel(""));

    gb.insets.top=2;
    gb.insets.left=5;
    gb.setX(0);
    gb.addY(new JLabel("ssh:"));

    gb.insets.left=1;
    gb.fill=gb.HORIZONTAL;
    gb.weightXY(0.3, 0);
    gb.addX(jcbPrevious);

    gb.insets.left=5;
    gb.weightXY(0.7, 0);
    jtfFile.setColumns(50);
    gb.addX(jtfFile);

    gb.weightXY(0, 0);
    gb.addX(jcbSudo);

    return jp;
  }
  private JPanel getErrorPanel() {
    JPanel panel=pnlError;
    GridBug gb=new GridBug(panel);
    gb.setInsets(5);
    gb.insets.top=0;
    gb.weightXY(1, 0);
    gb.anchor=gb.WEST;
    gb.add(lblError);
    return panel;
  }
  private JPanel getButtons() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    Insets insets=gb.insets;
    insets.top=5;
    insets.bottom=5;
    insets.left=5;
    insets.right=5;

    gb.add(btnOK);
    gb.addX(btnCancel);
    return panel;
  }
  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
  }

  private void listen(){
    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(true);}
    };
    btnOK.addActionListener(okAction);
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));

    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.easyCancel(btnCancel, cancelAction);
    win.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        click(false);
      }
    });
  }
  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          PopupTestContext ptc=new PopupTestContext();
          SSHOpenFrom w=new SSHOpenFrom(ptc.getPopupInfo(), ptc.getFontOptions());
          List<UserServer> uss=new ArrayList<>();

          System.out.println(w.show(false, uss));

          uss.add(new UserServer("mrderp", "suvuh.suv.com"));
          uss.add(new UserServer("trang", "woi.hoi.org"));
          System.out.println(w.show(true, uss));

          uss.add(new UserServer("sploong", "twabbada.lob.net"));
          System.out.println(w.show(false, uss));

        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

}

